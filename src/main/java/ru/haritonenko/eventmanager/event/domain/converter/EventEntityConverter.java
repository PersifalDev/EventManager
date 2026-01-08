package ru.haritonenko.eventmanager.event.domain.converter;

import org.springframework.stereotype.Component;
import ru.haritonenko.eventmanager.event.domain.db.entity.EventEntity;
import ru.haritonenko.eventmanager.event.domain.Event;

@Component
public class EventEntityConverter {

    public Event toDomain(EventEntity eventEntity) {
        return new Event(
                eventEntity.getId(),
                eventEntity.getName(),
                eventEntity.getOwner().getId().toString(),
                eventEntity.getMaxPlaces(),
                eventEntity.getOccupiedPlaces(),
                eventEntity.getDate(),
                eventEntity.getCost(),
                eventEntity.getDuration(),
                eventEntity.getLocation().getId(),
                eventEntity.getStatus()
        );
    }
}
