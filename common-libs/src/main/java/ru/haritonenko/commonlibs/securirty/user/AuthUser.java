package ru.haritonenko.commonlibs.securirty.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record AuthUser(
        @NotNull(message = "User id can not be null")
        Integer id,
        @NotBlank(message = "User login can not be blank")
        String login,
        @NotBlank(message = "User role can not be blank")
        String role
) {}