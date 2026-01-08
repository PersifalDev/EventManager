package ru.haritonenko.eventnotificator.domain;

import java.time.LocalDateTime;

public record EventNotification(
        Integer id,
        Integer userId,
        Integer eventId,
        LocalDateTime createdAt,
        boolean read,
        String message
) {
}
