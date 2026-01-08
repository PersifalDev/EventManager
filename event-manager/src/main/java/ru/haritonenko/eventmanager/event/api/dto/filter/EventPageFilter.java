package ru.haritonenko.eventmanager.event.api.dto.filter;

import jakarta.validation.constraints.Min;

public record EventPageFilter(
        @Min(value = 0, message = "Min number of page is 0")
        Integer pageNumber,
        @Min(value = 3, message = "Min size of page is 3")
        Integer pageSize
) {
}
