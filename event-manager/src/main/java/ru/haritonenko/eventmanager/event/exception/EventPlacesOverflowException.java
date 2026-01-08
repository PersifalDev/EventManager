package ru.haritonenko.eventmanager.event.exception;

public class EventPlacesOverflowException extends RuntimeException {
    public EventPlacesOverflowException(String message) {
        super(message);
    }
}
