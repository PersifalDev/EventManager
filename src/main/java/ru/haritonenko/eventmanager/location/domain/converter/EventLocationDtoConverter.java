package ru.haritonenko.eventmanager.location.domain.converter;

import org.springframework.stereotype.Component;
import ru.haritonenko.eventmanager.location.api.dto.EventLocationCreateRequestDto;
import ru.haritonenko.eventmanager.location.api.dto.EventLocationUpdateRequestDto;
import ru.haritonenko.eventmanager.location.domain.EventLocation;
import ru.haritonenko.eventmanager.location.api.dto.EventLocationDto;

@Component
public class EventLocationDtoConverter {

    public EventLocationDto toDto(EventLocation eventLocation) {
        return new EventLocationDto(
                eventLocation.id(),
                eventLocation.name(),
                eventLocation.address(),
                eventLocation.capacity(),
                eventLocation.description()
        );
    }

    public EventLocation fromUpdateDtoToDomain(
            EventLocationUpdateRequestDto eventLocationUpdateDto
    ) {
        return new EventLocation(
                eventLocationUpdateDto.id(),
                eventLocationUpdateDto.name(),
                eventLocationUpdateDto.address(),
                eventLocationUpdateDto.capacity(),
                eventLocationUpdateDto.description()
        );
    }

    public EventLocation fromCreateDtoToDomain(
            EventLocationCreateRequestDto eventLocationCreateDto
    ) {
        return new EventLocation(
                eventLocationCreateDto.id(),
                eventLocationCreateDto.name(),
                eventLocationCreateDto.address(),
                eventLocationCreateDto.capacity(),
                eventLocationCreateDto.description()
        );
    }
}
