package ru.haritonenko.eventmanager.event.registration.exception;

public class EventRegistrationNotFoundException extends RuntimeException {
    public EventRegistrationNotFoundException(String message) {
        super(message);
    }
}
