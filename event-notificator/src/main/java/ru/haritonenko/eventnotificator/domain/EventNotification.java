package ru.haritonenko.eventnotificator.domain;

import java.time.LocalDateTime;

public record EventNotification(
        Long id,
        Long userId,
        Long eventId,
        LocalDateTime createdAt,
        boolean read,
        String message
) {
}
