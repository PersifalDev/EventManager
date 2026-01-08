package ru.haritonenko.eventmanager.event.registration.domain.exception;

public class InvalidEventRegistrationStatusException extends RuntimeException {
    public InvalidEventRegistrationStatusException(String message) {
        super(message);
    }
}
