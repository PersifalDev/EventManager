package ru.haritonenko.eventmanager.event.domain.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import ru.haritonenko.eventmanager.event.api.dto.EventUpdateRequestDto;
import ru.haritonenko.eventmanager.event.domain.db.entity.EventEntity;

@Mapper(componentModel = "spring")
public interface EventUpdateMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "owner", ignore = true)
    @Mapping(target = "location", ignore = true)
    @Mapping(target = "registrations", ignore = true)
    @Mapping(target = "occupiedPlaces", ignore = true)
    @Mapping(target = "status", ignore = true)
    void updateEntity(@MappingTarget EventEntity entity, EventUpdateRequestDto dto);
}
