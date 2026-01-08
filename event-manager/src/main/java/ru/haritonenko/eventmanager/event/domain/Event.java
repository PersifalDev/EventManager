package ru.haritonenko.eventmanager.event.domain;

import lombok.Builder;
import ru.haritonenko.eventmanager.event.domain.status.EventStatus;

import java.math.BigDecimal;

@Builder
public record Event(
        Integer id,
        String name,
        String ownerId,
        Integer maxPlaces,
        Integer occupiedPlaces,
        String date,
        BigDecimal cost,
        Integer duration,
        Integer locationId,
        EventStatus status
) {
}
