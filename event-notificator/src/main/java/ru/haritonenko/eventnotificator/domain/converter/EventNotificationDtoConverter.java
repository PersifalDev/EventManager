package ru.haritonenko.eventnotificator.domain.converter;

import org.springframework.stereotype.Component;
import ru.haritonenko.eventnotificator.api.dto.EventNotificationDto;
import ru.haritonenko.eventnotificator.domain.EventNotification;

@Component
public class EventNotificationDtoConverter {

    public EventNotificationDto toDto(EventNotification eventNotification) {
        return EventNotificationDto.builder()
                .id(eventNotification.id())
                .eventId(eventNotification.eventId())
                .createdAt(eventNotification.createdAt())
                .read(eventNotification.read())
                .message(eventNotification.message())
                .build();
    }
}
