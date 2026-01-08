package ru.haritonenko.eventmanager.event.domain.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.haritonenko.eventmanager.event.domain.Event;
import ru.haritonenko.eventmanager.event.domain.db.entity.EventEntity;

@Mapper(componentModel = "spring")
public interface EventEntityMapper {

    @Mapping(target = "ownerId", source = "owner.id")
    @Mapping(target = "locationId", source = "location.id")
    Event toDomain(EventEntity entity);
}