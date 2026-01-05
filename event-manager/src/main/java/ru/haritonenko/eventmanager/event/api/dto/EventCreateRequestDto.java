package ru.haritonenko.eventmanager.event.api.dto;

import jakarta.validation.constraints.*;
import ru.haritonenko.eventmanager.event.domain.custom.validation.annotation.NotPastDateTime;

public record EventCreateRequestDto(
        @NotBlank(message = "Event name can not be blank")
        @Size(min = 1, max = 50, message = "Min name size is 1, max is 50")
        String name,
        @NotNull(message = "Event maxPlaces can not be null")
        @Min(value = 0, message = "Min count of maxPlaces is 0")
        Integer maxPlaces,
        @NotBlank(message = "Event date can not be blank")
        @NotPastDateTime(message = "date must be now or in the future")
        String date,
        @NotNull(message = "Event cost can not be null")
        @Positive(message = "Event cost can not be negative or zero")
        Integer cost,
        @NotNull(message = "Event duration can not be null")
        @Min(value = 30, message = "Min duration is 30")
        Integer duration,
        @NotNull(message = "Event locationId can not be null")
        @Min(value = 1, message = "Min locationId is 1")
        Integer locationId
) {
}
