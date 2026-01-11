package ru.haritonenko.eventnotificator.api.dto.filter;

import jakarta.validation.constraints.Min;

public record EventNotificationPageFilter(
        @Min(value = 0, message = "Min number of page is 0")
        Integer pageNumber,
        @Min(value = 3, message = "Min size of page is 3")
        Integer pageSize
) {
}
