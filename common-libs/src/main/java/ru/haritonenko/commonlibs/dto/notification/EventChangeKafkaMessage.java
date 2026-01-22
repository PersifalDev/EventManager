package ru.haritonenko.commonlibs.dto.notification;

import lombok.Builder;
import ru.haritonenko.commonlibs.dto.changes.EventFieldChange;

import java.util.List;

@Builder
public record EventChangeKafkaMessage(
        List<Long> users,
        Long ownerId,
        Long changedById,
        Long eventId,
        EventFieldChange<String> name,
        EventFieldChange<Integer> maxPlaces,
        EventFieldChange<String> time,
        EventFieldChange<Number> cost,
        EventFieldChange<Integer> duration,
        EventFieldChange<String> status,
        EventFieldChange<Long> locationId
) {
}
