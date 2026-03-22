package ru.haritonenko.eventmanager.location.domain;

import lombok.Builder;

@Builder(toBuilder = true)
public record EventLocation(
        Long id,
        String name,
        String address,
        Integer capacity,
        String description
) {
}
