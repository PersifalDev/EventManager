package ru.haritonenko.eventmanager.location.domain.mapper;

import org.mapstruct.Mapper;
import ru.haritonenko.eventmanager.location.api.dto.EventLocationCreateRequestDto;
import ru.haritonenko.eventmanager.location.api.dto.EventLocationDto;
import ru.haritonenko.eventmanager.location.api.dto.EventLocationUpdateRequestDto;
import ru.haritonenko.eventmanager.location.domain.EventLocation;

@Mapper(componentModel = "spring")
public interface EventLocationDtoMapper {

    EventLocationDto toDto(EventLocation domain);

    EventLocation fromCreateDto(EventLocationCreateRequestDto dto);

    EventLocation fromUpdateDto(EventLocationUpdateRequestDto dto);
}