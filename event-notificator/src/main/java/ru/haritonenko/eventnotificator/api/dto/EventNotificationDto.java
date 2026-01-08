package ru.haritonenko.eventnotificator.api.dto;

import java.time.LocalDateTime;

public record EventNotificationDto(
        Integer id,
        Integer eventId,
        LocalDateTime createdAt,
        boolean read,
        String message
) {
}