package ru.haritonenko.eventmanager.event.api.dto.filter;

import jakarta.validation.constraints.*;
import ru.haritonenko.eventmanager.event.domain.status.EventStatus;

public record EventSearchRequestDto(
        @Size(min = 1, max = 50, message = "Min name size is 1, max is 50")
        String name,
        @Min(value = 0, message = "Min count of minPlaces is 0")
        Integer placesMin,
        @Min(value = 0, message = "Min count of maxPlaces is 0")
        Integer placesMax,
        String dateStartAfter,
        String dateStartBefore,
        @Positive(message = "Event cost can not be negative or zero")
        Number costMin,
        @Positive(message = "Event cost can not be negative or zero")
        Number costMax,
        @Min(value = 30, message = "Min durationMin is 30")
        Integer durationMin,
        @Min(value = 30, message = "Min durationMax is 30")
        Integer durationMax,
        @Min(value = 1, message = "Min locationId is 1")
        Integer locationId,
        EventStatus eventStatus
) {
}
