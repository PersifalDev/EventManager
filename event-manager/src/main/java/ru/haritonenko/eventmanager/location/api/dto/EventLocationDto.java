package ru.haritonenko.eventmanager.location.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.*;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record EventLocationDto(
        @Null
        Integer id,
        @NotBlank(message = "Location name can not be blank")
        @Size(min = 1, max = 40, message = "Min name size is 1, max is 40")
        String name,
        @NotBlank(message = "Location address can not be blank")
        @Size(min = 5, max = 30, message = "Min address size is 5, max is 30")
        String address,
        @NotNull(message = "Location capacity can not be null")
        @Min(value = 5, message = "Min location capacity is 5")
        Integer capacity,
        @NotBlank(message = "Location description can not be blank")
        @Size(min = 10, max = 100, message = "Min description size is 10, max is 100")
        String description
) {
}