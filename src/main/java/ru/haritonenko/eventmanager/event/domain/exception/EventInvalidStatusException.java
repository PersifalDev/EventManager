package ru.haritonenko.eventmanager.event.domain.exception;

public class EventInvalidStatusException extends RuntimeException {
    public EventInvalidStatusException(String message) {
        super(message);
    }
}
