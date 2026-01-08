package ru.haritonenko.eventmanager.event.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.*;
import ru.haritonenko.eventmanager.event.domain.custom.validation.annotation.NotPastDateTime;
import ru.haritonenko.eventmanager.event.domain.status.EventStatus;

import java.math.BigDecimal;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record EventDto(
        @Null
        Integer id,
        @NotBlank(message = "Event name can not be blank")
        @Size(min = 1, max = 50, message = "Min name size is 1, max is 50")
        String name,
        @NotBlank(message = "Event ownerId can not be blank")
        String ownerId,
        @NotNull(message = "Event maxPlaces can not be null")
        @Min(value = 0, message = "Min count of maxPlaces is 0")
        Integer maxPlaces,
        @NotNull(message = "Event occupiedPlaces can not be null")
        @Min(value = 0, message = "Min count of occupiedPlaces is 0")
        Integer occupiedPlaces,
        @NotBlank(message = "Event date can not be blank")
        @NotPastDateTime(message = "date must be now or in the future")
        String date,
        @NotNull(message = "Event cost can not be null")
        @Positive(message = "Event cost can not be negative or zero")
        BigDecimal cost,
        @NotNull(message = "Event duration can not be null")
        @Min(value = 30, message = "Min duration is 30")
        Integer duration,
        @NotNull(message = "Event locationId can not be null")
        Integer locationId,
        @NotNull(message = "Event status can not be null")
        EventStatus status
) {
}
