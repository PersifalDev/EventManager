package ru.haritonenko.eventmanager.event.registration.exception;

public class InvalidEventRegistrationStatusException extends RuntimeException {
    public InvalidEventRegistrationStatusException(String message) {
        super(message);
    }
}
