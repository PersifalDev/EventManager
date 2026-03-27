package ru.haritonenko.eventnotificator.domain.service;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import ru.haritonenko.commonlibs.dto.changes.EventFieldChange;
import ru.haritonenko.commonlibs.dto.notification.EventChangeKafkaMessage;
import ru.haritonenko.commonlibs.securirty.user.AuthUser;
import ru.haritonenko.eventnotificator.AbstractIntegrationTest;
import ru.haritonenko.eventnotificator.api.dto.filter.EventNotificationPageFilter;
import ru.haritonenko.eventnotificator.cache.locking.RedisLockManager;
import ru.haritonenko.eventnotificator.domain.EventNotification;
import ru.haritonenko.eventnotificator.domain.db.entity.EventNotificationEntity;
import ru.haritonenko.eventnotificator.domain.db.repository.EventNotificationRepository;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@Import(EventNotificationServiceIntegrationTest.TestConfig.class)
class EventNotificationServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private EventNotificationService eventNotificationService;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private EventNotificationRepository eventNotificationRepository;

    @MockitoBean
    private RedisTemplate<String, EventNotificationEntity> redisTemplate;

    @MockitoBean
    private ListOperations<String, EventNotificationEntity> listOperations;

    @MockitoBean
    private RedisLockManager redisLockManager;

    @MockitoBean
    private StringRedisTemplate stringRedisTemplate;

    @TestConfiguration
    static class TestConfig {

        @Bean
        RedisConnectionFactory redisConnectionFactory() {
            return Mockito.mock(RedisConnectionFactory.class);
        }

        @Bean
        ReactiveRedisConnectionFactory reactiveRedisConnectionFactory() {
            return Mockito.mock(ReactiveRedisConnectionFactory.class);
        }
    }

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForList()).thenReturn(listOperations);
        when(listOperations.range(anyString(), anyLong(), anyLong())).thenReturn(List.of());
        when(listOperations.size(anyString())).thenReturn(0L);
    }

    @Transactional
    @Test
    void shouldSuccessfullyFindUnreadNotificationsForUser() {
        eventNotificationRepository.save(
                EventNotificationEntity.builder()
                        .userId(1L)
                        .eventId(10L)
                        .createdAt(LocalDateTime.now())
                        .read(false)
                        .message("first-message")
                        .build()
        );

        eventNotificationRepository.save(
                EventNotificationEntity.builder()
                        .userId(1L)
                        .eventId(11L)
                        .createdAt(LocalDateTime.now())
                        .read(false)
                        .message("second-message")
                        .build()
        );

        List<EventNotification> notifications = eventNotificationService.findUnreadNotificationsForUser(
                new AuthUser(1L, "test-login", "USER"),
                new EventNotificationPageFilter(null, null)
        );

        assertEquals(2, notifications.size());
    }

    @Transactional
    @Test
    void shouldSuccessfullyMarkNotificationsAsRead() {
        EventNotificationEntity firstNotification = eventNotificationRepository.save(
                EventNotificationEntity.builder()
                        .userId(1L)
                        .eventId(10L)
                        .createdAt(LocalDateTime.now())
                        .read(false)
                        .message("first-message")
                        .build()
        );

        EventNotificationEntity secondNotification = eventNotificationRepository.save(
                EventNotificationEntity.builder()
                        .userId(1L)
                        .eventId(11L)
                        .createdAt(LocalDateTime.now())
                        .read(false)
                        .message("second-message")
                        .build()
        );

        assertDoesNotThrow(() ->
                eventNotificationService.markNotificationsAsRead(
                        new AuthUser(1L, "test-login", "USER"),
                        List.of(firstNotification.getId(), secondNotification.getId())
                )
        );

        entityManager.flush();
        entityManager.clear();

        EventNotificationEntity updatedFirstNotification =
                eventNotificationRepository.findById(firstNotification.getId()).orElseThrow();
        EventNotificationEntity updatedSecondNotification =
                eventNotificationRepository.findById(secondNotification.getId()).orElseThrow();

        assertEquals(true, updatedFirstNotification.isRead());
        assertEquals(true, updatedSecondNotification.isRead());
    }

    @Transactional
    @Test
    void shouldSuccessfullySaveNotificationsFromKafka() {
        EventChangeKafkaMessage message = EventChangeKafkaMessage.builder()
                .users(List.of(1L, 2L, 3L))
                .ownerId(10L)
                .changedById(11L)
                .eventId(15L)
                .name(new EventFieldChange<>("old-name", "new-name"))
                .build();

        eventNotificationService.saveNotificationsFromKafka(message);

        List<EventNotificationEntity> notifications = eventNotificationRepository.findAll();

        assertEquals(3, notifications.size());
        assertEquals(true, notifications.stream().allMatch(notification -> !notification.isRead()));
    }
}