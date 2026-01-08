package ru.haritonenko.eventmanager.event.domain.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.haritonenko.eventmanager.event.api.dto.EventCreateRequestDto;
import ru.haritonenko.eventmanager.event.domain.db.entity.EventEntity;
import ru.haritonenko.eventmanager.event.domain.status.EventStatus;
import ru.haritonenko.eventmanager.location.domain.db.entity.EventLocationEntity;
import ru.haritonenko.eventmanager.user.domain.db.entity.UserEntity;

import java.math.BigDecimal;
import java.util.ArrayList;

@Mapper(componentModel = "spring", imports = {BigDecimal.class, ArrayList.class, EventStatus.class})
public interface EventCreateMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "name", source = "dto.name")
    @Mapping(target = "maxPlaces", source = "dto.maxPlaces")
    @Mapping(target = "date", source = "dto.date")
    @Mapping(target = "duration", source = "dto.duration")
    @Mapping(target = "owner", source = "owner")
    @Mapping(target = "location", source = "location")
    @Mapping(target = "registrations", expression = "java(new ArrayList<>())")
    @Mapping(target = "occupiedPlaces", constant = "0")
    @Mapping(target = "cost", expression = "java(BigDecimal.valueOf(dto.cost()))")
    @Mapping(target = "status", expression = "java(EventStatus.WAIT_START)")
    EventEntity toEntity(EventCreateRequestDto dto, UserEntity owner, EventLocationEntity location);
}
