package ru.haritonenko.commonlibs.error.exceptions.user_exception.exception;

public class UserAlreadyRegisteredOnEventException extends RuntimeException {
    public UserAlreadyRegisteredOnEventException(String message) {
        super(message);
    }
}
