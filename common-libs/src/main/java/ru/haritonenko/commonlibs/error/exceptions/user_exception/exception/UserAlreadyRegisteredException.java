package ru.haritonenko.commonlibs.error.exceptions.user_exception.exception;

public class UserAlreadyRegisteredException extends RuntimeException {
    public UserAlreadyRegisteredException(String message) {
        super(message);
    }
}
