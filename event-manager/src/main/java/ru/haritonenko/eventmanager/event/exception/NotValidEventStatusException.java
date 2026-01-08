package ru.haritonenko.eventmanager.event.exception;

public class NotValidEventStatusException extends RuntimeException {
    public NotValidEventStatusException(String message) {
        super(message);
    }
}
