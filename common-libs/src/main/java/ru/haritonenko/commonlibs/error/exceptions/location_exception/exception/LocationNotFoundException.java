package ru.haritonenko.commonlibs.error.exceptions.location_exception.exception;

public class LocationNotFoundException extends RuntimeException {
    public LocationNotFoundException(String message) {
        super(message);
    }
}
