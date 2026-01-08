package ru.haritonenko.eventmanager.user.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record UserDto(
        Integer id,
        String login
) {
}
