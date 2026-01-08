package ru.haritonenko.eventmanager.location.domain;

public record EventLocation(
        Integer id,
        String name,
        String address,
        Integer capacity,
        String description
) {
}
