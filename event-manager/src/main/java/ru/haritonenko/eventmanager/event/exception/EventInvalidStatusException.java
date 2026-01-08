package ru.haritonenko.eventmanager.event.exception;

public class EventInvalidStatusException extends RuntimeException {
    public EventInvalidStatusException(String message) {
        super(message);
    }
}
