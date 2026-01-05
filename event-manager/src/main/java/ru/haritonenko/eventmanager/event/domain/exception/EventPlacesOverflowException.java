package ru.haritonenko.eventmanager.event.domain.exception;

public class EventPlacesOverflowException extends RuntimeException {
    public EventPlacesOverflowException(String message) {
        super(message);
    }
}
