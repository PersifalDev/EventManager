package ru.haritonenko.eventmanager.user.api.dto.registration;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserRegistration(
        @NotBlank(message = "User login can't be blank")
        @Size(min = 4, max = 50, message = "Min login size is 4, max is 50")
        String login,
        @NotBlank(message = "User password can't be blank")
        @Size(min = 4, max = 50, message = "Min password size is 4, max is 50")
        String password,
        @Min(value = 18, message = "Min age size is 18")
        int age
) {
}
