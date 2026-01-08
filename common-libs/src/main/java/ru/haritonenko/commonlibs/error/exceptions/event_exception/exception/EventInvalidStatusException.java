package ru.haritonenko.commonlibs.error.exceptions.event_exception.exception;

public class EventInvalidStatusException extends RuntimeException {
    public EventInvalidStatusException(String message) {
        super(message);
    }
}
