package ru.haritonenko.eventnotificator.api.dto;

import java.util.List;

public record MarkNotificationsReadRequest(
        List<Long> notificationIds
) {
}