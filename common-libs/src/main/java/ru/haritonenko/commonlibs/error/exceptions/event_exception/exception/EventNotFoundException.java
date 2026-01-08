package ru.haritonenko.commonlibs.error.exceptions.event_exception.exception;

public class EventNotFoundException extends RuntimeException {
    public EventNotFoundException(String message) {
        super(message);
    }
}
