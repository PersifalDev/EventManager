package ru.haritonenko.eventmanager.user.domain.exception;

public class UserAlreadyRegisteredOnEventException extends RuntimeException {
    public UserAlreadyRegisteredOnEventException(String message) {
        super(message);
    }
}
