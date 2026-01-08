package ru.haritonenko.eventnotificator.domain.mapper;

import org.mapstruct.Mapper;
import ru.haritonenko.eventnotificator.api.dto.EventNotificationDto;
import ru.haritonenko.eventnotificator.domain.EventNotification;

@Mapper(componentModel = "spring")
public interface EventNotificationDtoMapper {

    EventNotificationDto toDto(EventNotification domain);
}
