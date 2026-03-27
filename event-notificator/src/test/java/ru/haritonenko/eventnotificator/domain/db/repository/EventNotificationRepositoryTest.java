package ru.haritonenko.eventnotificator.domain.db.repository;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.haritonenko.eventnotificator.domain.db.entity.EventNotificationEntity;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class EventNotificationRepositoryTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES_CONTAINER =
            new PostgreSQLContainer<>("postgres:15.3")
                    .withDatabaseName("eventNotificationTestDb")
                    .withUsername("testNotification")
                    .withPassword("testNotification")
                    .withReuse(true);

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES_CONTAINER::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES_CONTAINER::getUsername);
        registry.add("spring.datasource.password", POSTGRES_CONTAINER::getPassword);
        registry.add("spring.datasource.driver-class-name", POSTGRES_CONTAINER::getDriverClassName);
    }

    @Autowired
    private EventNotificationRepository eventNotificationRepository;

    private EventNotificationEntity notificationEntity;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void setUp() {
        notificationEntity = EventNotificationEntity.builder()
                .userId(1L)
                .eventId(10L)
                .createdAt(LocalDateTime.now())
                .read(false)
                .message("Event has been changed")
                .build();
    }

    @Test
    void shouldSaveNotificationAndGenerateId() {
        EventNotificationEntity savedNotification = eventNotificationRepository.save(notificationEntity);

        assertNotNull(savedNotification.getId());
        assertEquals(1L, savedNotification.getUserId());
        assertEquals(10L, savedNotification.getEventId());
        assertEquals("Event has been changed", savedNotification.getMessage());
        assertEquals(false, savedNotification.isRead());
    }

    @Test
    void shouldFindAllUnreadNotificationsByUserId() {
        eventNotificationRepository.save(notificationEntity);
        eventNotificationRepository.save(
                EventNotificationEntity.builder()
                        .userId(1L)
                        .eventId(11L)
                        .createdAt(LocalDateTime.now())
                        .read(false)
                        .message("Second message")
                        .build()
        );
        eventNotificationRepository.save(
                EventNotificationEntity.builder()
                        .userId(1L)
                        .eventId(12L)
                        .createdAt(LocalDateTime.now())
                        .read(true)
                        .message("Read message")
                        .build()
        );

        List<EventNotificationEntity> unreadNotifications =
                eventNotificationRepository.findAllUnredNotificationsByUserId(1L);

        assertEquals(2, unreadNotifications.size());
        assertTrue(unreadNotifications.stream().allMatch(notification -> !notification.isRead()));
    }

    @Test
    void shouldMarkUserNotificationsAsRead() {
        EventNotificationEntity firstNotification = eventNotificationRepository.save(notificationEntity);
        EventNotificationEntity secondNotification = eventNotificationRepository.save(
                EventNotificationEntity.builder()
                        .userId(1L)
                        .eventId(11L)
                        .createdAt(LocalDateTime.now())
                        .read(false)
                        .message("Second message")
                        .build()
        );

        eventNotificationRepository.markUserNotificationsAsRead(
                1L,
                List.of(firstNotification.getId(), secondNotification.getId())
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

    @Test
    void shouldDeleteNotificationsByCreatedAtBefore() {
        EventNotificationEntity oldNotification = eventNotificationRepository.save(
                EventNotificationEntity.builder()
                        .userId(1L)
                        .eventId(10L)
                        .createdAt(LocalDateTime.now().minusDays(10))
                        .read(false)
                        .message("Old message")
                        .build()
        );

        EventNotificationEntity newNotification = eventNotificationRepository.save(
                EventNotificationEntity.builder()
                        .userId(1L)
                        .eventId(11L)
                        .createdAt(LocalDateTime.now())
                        .read(false)
                        .message("New message")
                        .build()
        );

        long deletedRows = eventNotificationRepository.deleteByCreatedAtBefore(LocalDateTime.now().minusDays(3));

        assertEquals(1, deletedRows);
        assertTrue(eventNotificationRepository.findById(oldNotification.getId()).isEmpty());
        assertTrue(eventNotificationRepository.findById(newNotification.getId()).isPresent());
    }
}