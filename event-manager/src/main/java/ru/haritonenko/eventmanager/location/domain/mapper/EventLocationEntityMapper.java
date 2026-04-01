package ru.haritonenko.eventmanager.location.domain.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.haritonenko.eventmanager.location.domain.EventLocation;
import ru.haritonenko.eventmanager.location.domain.db.entity.EventLocationEntity;

@Mapper(componentModel = "spring")
public interface EventLocationEntityMapper {

    EventLocation toDomain(EventLocationEntity entity);

    @Mapping(target = "events", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    EventLocationEntity toEntity(EventLocation domain);
}
