package ru.haritonenko.commonlibs.error.exceptions.user_exception.exception;

public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(String message) {
        super(message);
    }
}
