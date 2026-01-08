package ru.haritonenko.commonlibs.error.exceptions.registration_exception.exception;

public class InvalidEventRegistrationStatusException extends RuntimeException {
    public InvalidEventRegistrationStatusException(String message) {
        super(message);
    }
}
