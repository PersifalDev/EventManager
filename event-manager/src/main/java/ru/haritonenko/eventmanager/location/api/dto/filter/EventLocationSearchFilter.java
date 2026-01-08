package ru.haritonenko.eventmanager.location.api.dto.filter;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record EventLocationSearchFilter(
        @Size(min = 1, max = 40, message = "Min name size is 1, max is 40")
        String name,
        @Size(min = 5, max = 30, message = "Min address size is 5, max is 30")
        String address,
        @Min(value = 0, message = "Min number of page is 0")
        Integer pageNumber,
        @Min(value = 3, message = "Min size of page is 3")
        Integer pageSize
) {
}
