package ru.haritonenko.eventnotificator.domain.converter;

import org.springframework.stereotype.Component;
import ru.haritonenko.eventnotificator.domain.EventNotification;
import ru.haritonenko.eventnotificator.domain.db.entity.EventNotificationEntity;

@Component
public class EventNotificationDomainConverter {

    public EventNotification toDomain(EventNotificationEntity eventNotificationEntity) {
        return EventNotification.builder()
                .id(eventNotificationEntity.getId())
                .eventId(eventNotificationEntity.getEventId())
                .createdAt(eventNotificationEntity.getCreatedAt())
                .read(eventNotificationEntity.isRead())
                .message(eventNotificationEntity.getMessage())
                .build();
    }
}
