package ru.haritonenko.eventmanager.location.domain;

import lombok.Builder;

@Builder
public record EventLocation(
        Integer id,
        String name,
        String address,
        Integer capacity,
        String description
) {
}
