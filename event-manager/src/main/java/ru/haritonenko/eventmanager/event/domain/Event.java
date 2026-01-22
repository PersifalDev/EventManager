package ru.haritonenko.eventmanager.event.domain;
import ru.haritonenko.eventmanager.event.domain.status.EventStatus;
import java.math.BigDecimal;

public record Event(
        Long id,
        String name,
        String ownerId,
        Integer maxPlaces,
        Integer occupiedPlaces,
        String date,
        BigDecimal cost,
        Integer duration,
        Long locationId,
        EventStatus status
) {
}
