package ru.haritonenko.eventmanager.user.domain;

import ru.haritonenko.eventmanager.user.domain.role.UserRole;

public record User(
        Long id,
        String login,
        int age,
        UserRole role
) {
}
