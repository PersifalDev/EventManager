package ru.haritonenko.eventnotificator.domain.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import ru.haritonenko.commonlibs.dto.changes.EventFieldChange;
import ru.haritonenko.commonlibs.dto.notification.EventChangeKafkaMessage;
import ru.haritonenko.commonlibs.securirty.user.AuthUser;
import ru.haritonenko.eventnotificator.api.dto.filter.EventNotificationPageFilter;
import ru.haritonenko.eventnotificator.domain.EventNotification;
import ru.haritonenko.eventnotificator.domain.db.entity.EventNotificationEntity;
import ru.haritonenko.eventnotificator.domain.db.repository.EventNotificationRepository;
import ru.haritonenko.eventnotificator.domain.mapper.EventNotificationEntityMapper;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventNotificationServiceUnitTest {

    @Mock
    private EventNotificationRepository notificationRepository;

    @Mock
    private EventNotificationEntityMapper mapper;

    @Mock
    private RedisTemplate<String, EventNotificationEntity> redisTemplate;

    @Mock
    private ListOperations<String, EventNotificationEntity> listOperations;

    @InjectMocks
    private EventNotificationService eventNotificationService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(eventNotificationService, "defaultPageSize", 5);
        ReflectionTestUtils.setField(eventNotificationService, "defaultPageNumber", 0);
        ReflectionTestUtils.setField(eventNotificationService, "cacheTtl", Duration.ofSeconds(30));

    }

    @Test
    void shouldSuccessfullyReturnUnreadNotificationsFromCache() {

        AuthUser authUser = new AuthUser(1L, "test-login", "USER");

        EventNotificationEntity cachedEntity = EventNotificationEntity.builder()
                .id(1L)
                .userId(1L)
                .eventId(10L)
                .createdAt(LocalDateTime.now())
                .read(false)
                .message("cached-message")
                .build();

        EventNotification domain = new EventNotification(
                1L,
                1L,
                10L,
                cachedEntity.getCreatedAt(),
                false,
                "cached-message"
        );

        when(redisTemplate.opsForList()).thenReturn(listOperations);
        when(listOperations.range("unread-notifications:1", 0, 4)).thenReturn(List.of(cachedEntity));

        when(listOperations.range("unread-notifications:1", 0, 4)).thenReturn(List.of(cachedEntity));
        when(mapper.toDomain(cachedEntity)).thenReturn(domain);

        List<EventNotification> notifications = eventNotificationService.findUnreadNotificationsForUser(
                authUser,
                new EventNotificationPageFilter(null, null)
        );

        assertEquals(1, notifications.size());
        assertEquals(1L, notifications.get(0).id());
        assertEquals("cached-message", notifications.get(0).message());
        verify(notificationRepository, never()).findAllUnredNotificationsByUserId(anyLong());
    }

    @Test
    void shouldSuccessfullyReturnUnreadNotificationsFromDatabaseAndPutThemIntoCache() {
        AuthUser authUser = new AuthUser(1L, "test-login", "USER");

        EventNotificationEntity dbEntity = EventNotificationEntity.builder()
                .id(1L)
                .userId(1L)
                .eventId(10L)
                .createdAt(LocalDateTime.now())
                .read(false)
                .message("db-message")
                .build();

        EventNotification domain = new EventNotification(
                1L,
                1L,
                10L,
                dbEntity.getCreatedAt(),
                false,
                "db-message"
        );

        when(redisTemplate.opsForList()).thenReturn(listOperations);
        when(listOperations.range("unread-notifications:1", 0, 4)).thenReturn(List.of());
        when(listOperations.size("unread-notifications:1")).thenReturn(0L);
        when(notificationRepository.findAllUnredNotificationsByUserId(1L)).thenReturn(List.of(dbEntity));
        when(mapper.toDomain(dbEntity)).thenReturn(domain);

        List<EventNotification> notifications = eventNotificationService.findUnreadNotificationsForUser(
                authUser,
                new EventNotificationPageFilter(null, null)
        );

        assertEquals(1, notifications.size());
        assertEquals("db-message", notifications.get(0).message());

        verify(redisTemplate).delete("unread-notifications:1");
        verify(listOperations).rightPushAll("unread-notifications:1", List.of(dbEntity));
        verify(redisTemplate).expire("unread-notifications:1", Duration.ofSeconds(30));
    }

    @Test
    void shouldSuccessfullyMarkNotificationsAsReadAndInvalidateCache() {
        AuthUser authUser = new AuthUser(1L, "test-login", "USER");

        assertDoesNotThrow(() ->
                eventNotificationService.markNotificationsAsRead(authUser, List.of(1L, 2L, 3L))
        );

        verify(notificationRepository).markUserNotificationsAsRead(1L, List.of(1L, 2L, 3L));
        verify(redisTemplate).delete("unread-notifications:1");
    }

    @Test
    void shouldDoNothingWhenNotificationIdsEmptyWhileMarkAsRead() {
        AuthUser authUser = new AuthUser(1L, "test-login", "USER");

        assertDoesNotThrow(() ->
                eventNotificationService.markNotificationsAsRead(authUser, List.of())
        );

        verify(notificationRepository, never()).markUserNotificationsAsRead(anyLong(), anyList());
        verify(redisTemplate, never()).delete(anyString());
    }

    @Test
    void shouldSuccessfullySaveNotificationsFromKafka() {
        EventChangeKafkaMessage message = EventChangeKafkaMessage.builder()
                .users(List.of(1L, 2L, 2L, 3L))
                .ownerId(10L)
                .changedById(11L)
                .eventId(15L)
                .name(new EventFieldChange<>("old-name", "new-name"))
                .build();

        assertDoesNotThrow(() -> eventNotificationService.saveNotificationsFromKafka(message));

        verify(notificationRepository).saveAll(anyCollection());
        verify(redisTemplate).delete("unread-notifications:1");
        verify(redisTemplate).delete("unread-notifications:2");
        verify(redisTemplate).delete("unread-notifications:3");
    }

    @Test
    void shouldThrowIllegalStateExceptionWhenUserIsNullWhileSearchingUnreadNotifications() {
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> eventNotificationService.findUnreadNotificationsForUser(
                        null,
                        new EventNotificationPageFilter(null, null)
                )
        );

        assertEquals("Authentication not present", exception.getMessage());
    }

    @Test
    void shouldThrowIllegalStateExceptionWhenUserIsNullWhileMarkingAsRead() {
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> eventNotificationService.markNotificationsAsRead(
                        null,
                        List.of(1L)
                )
        );

        assertEquals("Authentication not present", exception.getMessage());
    }
}