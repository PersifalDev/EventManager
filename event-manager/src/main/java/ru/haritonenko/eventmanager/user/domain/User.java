package ru.haritonenko.eventmanager.user.domain;

import lombok.Builder;
import ru.haritonenko.eventmanager.user.domain.role.UserRole;

@Builder
public record User(
        Integer id,
        String login,
        int age,
        UserRole role
) {
}
