package ru.haritonenko.eventnotificator.api.dto;

import java.time.LocalDateTime;

public record EventNotificationDto(
        Long id,
        Long eventId,
        LocalDateTime createdAt,
        boolean read,
        String message
) {
}