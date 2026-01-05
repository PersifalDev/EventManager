package ru.haritonenko.eventmanager.event.domain.converter;

import org.springframework.stereotype.Component;
import ru.haritonenko.eventmanager.event.api.dto.EventDto;
import ru.haritonenko.eventmanager.event.domain.Event;

@Component
public class EventDtoConverter {

    public EventDto toDto(Event event) {
        return new EventDto(
                event.id(),
                event.name(),
                event.ownerId(),
                event.maxPlaces(),
                event.occupiedPlaces(),
                event.date(),
                event.cost(),
                event.duration(),
                event.locationId(),
                event.status()
        );
    }
}
