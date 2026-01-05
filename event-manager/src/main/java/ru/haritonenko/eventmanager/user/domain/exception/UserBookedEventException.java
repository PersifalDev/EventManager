package ru.haritonenko.eventmanager.user.domain.exception;

public class UserBookedEventException extends RuntimeException {
    public UserBookedEventException(String message) {
        super(message);
    }
}
