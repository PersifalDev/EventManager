package ru.haritonenko.eventnotificator.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.haritonenko.eventnotificator.domain.db.repository.EventNotificationRepository;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationCleanupScheduler {

    private final EventNotificationRepository notificationRepository;

    @Value("${scheduler.notifications.ttl-days:7}")
    private int ttlDays;

    @Transactional
    @Scheduled(cron = "${scheduler.notifications.cleanup-cron}")
    public void deleteOldNotifications() {
        LocalDateTime thresholdTime = LocalDateTime.now().minusDays(ttlDays);
        long deleted = notificationRepository.deleteByCreatedAtBefore(thresholdTime);
        if (deleted > 0) {
            log.info("Deleted {} notifications older than {} days (before {})", deleted, ttlDays, thresholdTime);
        }
    }
}
