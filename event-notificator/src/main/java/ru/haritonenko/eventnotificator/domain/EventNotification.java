package ru.haritonenko.eventnotificator.domain;


import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record EventNotification(
        Integer id,
        Integer userId,
        Integer eventId,
        LocalDateTime createdAt,
        boolean read,
        String message
) {}
