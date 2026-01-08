package ru.haritonenko.commonlibs.error.exceptions.registration_exception.exception;

public class EventRegistrationNotFoundException extends RuntimeException {
    public EventRegistrationNotFoundException(String message) {
        super(message);
    }
}
