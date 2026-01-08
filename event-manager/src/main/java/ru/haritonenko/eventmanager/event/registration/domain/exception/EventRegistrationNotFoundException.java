package ru.haritonenko.eventmanager.event.registration.domain.exception;

public class EventRegistrationNotFoundException extends RuntimeException {
    public EventRegistrationNotFoundException(String message) {
        super(message);
    }
}
