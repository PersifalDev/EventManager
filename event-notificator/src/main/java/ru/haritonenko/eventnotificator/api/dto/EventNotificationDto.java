package ru.haritonenko.eventnotificator.api.dto;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record EventNotificationDto(
        Integer id,
        Integer eventId,
        LocalDateTime createdAt,
        boolean read,
        String message
) {}