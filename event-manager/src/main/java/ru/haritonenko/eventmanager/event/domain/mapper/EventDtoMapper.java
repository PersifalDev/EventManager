package ru.haritonenko.eventmanager.event.domain.mapper;

import org.mapstruct.Mapper;
import ru.haritonenko.eventmanager.event.api.dto.EventDto;
import ru.haritonenko.eventmanager.event.domain.Event;

@Mapper(componentModel = "spring")
public interface EventDtoMapper {

    EventDto toDto(Event event);
}
