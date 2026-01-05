package ru.haritonenko.eventmanager.location.domain.converter;

import org.springframework.stereotype.Component;
import ru.haritonenko.eventmanager.location.domain.EventLocation;
import ru.haritonenko.eventmanager.location.domain.db.entity.EventLocationEntity;

@Component
public class EventLocationEntityConverter {

    public EventLocation toDomain(EventLocationEntity eventLocationEntity) {
        return new EventLocation(
                eventLocationEntity.getId(),
                eventLocationEntity.getName(),
                eventLocationEntity.getAddress(),
                eventLocationEntity.getCapacity(),
                eventLocationEntity.getDescription()
        );
    }
}
