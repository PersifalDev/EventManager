package ru.haritonenko.eventnotificator.cache.locking.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import ru.haritonenko.commonlibs.securirty.user.AuthUser;
import ru.haritonenko.eventnotificator.api.dto.MarkNotificationsReadRequest;
import ru.haritonenko.eventnotificator.cache.locking.RedisLockManager;
import ru.haritonenko.eventnotificator.domain.service.EventNotificationService;
import ru.haritonenko.eventnotificator.security.service.AuthUserService;

import java.time.Duration;

import static java.util.Objects.isNull;

@Slf4j
@RestController
@RequestMapping("/notifications/lock")
@RequiredArgsConstructor
public class EventNotificationUpdateLockingController {

    private final EventNotificationService eventNotificationService;
    private final AuthUserService authUserService;
    private final RedisLockManager redisLockManager;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void markNotificationsRead(
            @RequestBody @Valid MarkNotificationsReadRequest request,
            @RequestParam(defaultValue = "500") long workMs
    ) {
        log.info("Post request for marking unread notifications for user with locking");
        AuthUser user = getAuthenticatedUser();

        String lockKey = "notifications:mark-read:user:" + user.id();
        String lockId = redisLockManager.tryLock(lockKey, Duration.ofSeconds(30));

        if (isNull(lockId)) {
            throw new ResponseStatusException(
                    HttpStatus.LOCKED,
                    "Lock has been captured for object %s. Try later".formatted(lockKey)
            );
        }

        try {
            if (workMs > 0) {
                try {
                    Thread.sleep(workMs);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            }

            eventNotificationService.markNotificationsAsRead(user, request.notificationIds());
            log.info("Notifications marked as read");
        } finally {
            redisLockManager.unlockLock(lockKey, lockId);
        }
    }

    private AuthUser getAuthenticatedUser() {
        return authUserService.getCurrentAuthenticatedUser();
    }
}
