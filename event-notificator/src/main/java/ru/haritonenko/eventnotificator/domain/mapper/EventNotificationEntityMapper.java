package ru.haritonenko.eventnotificator.domain.mapper;

import org.mapstruct.Mapper;
import ru.haritonenko.eventnotificator.domain.EventNotification;
import ru.haritonenko.eventnotificator.domain.db.entity.EventNotificationEntity;

@Mapper(componentModel = "spring")
public interface EventNotificationEntityMapper {

    EventNotification toDomain(EventNotificationEntity entity);
}
