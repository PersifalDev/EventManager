package ru.haritonenko.eventmanager.error.handler;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.haritonenko.commonlibs.dto.error.ErrorMessageResponse;
import ru.haritonenko.eventmanager.event.exception.EventCountPlacesUpdateException;
import ru.haritonenko.eventmanager.event.exception.NotValidEventStatusException;
import ru.haritonenko.eventmanager.event.exception.EventNotFoundException;
import ru.haritonenko.eventmanager.event.exception.EventPlacesOverflowException;
import ru.haritonenko.eventmanager.location.domain.exception.LocationCountPlacesException;
import ru.haritonenko.eventmanager.location.domain.exception.LocationNotFoundException;
import ru.haritonenko.eventmanager.event.registration.domain.exception.EventRegistrationNotFoundException;
import ru.haritonenko.eventmanager.event.registration.domain.exception.InvalidEventRegistrationStatusException;
import ru.haritonenko.eventmanager.user.domain.exception.UserAlreadyRegisteredException;
import ru.haritonenko.eventmanager.user.domain.exception.UserAlreadyRegisteredOnEventException;
import ru.haritonenko.eventmanager.user.domain.exception.UserBookedEventException;
import ru.haritonenko.eventmanager.user.domain.exception.UserNotFoundException;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorMessageResponse> handleValidationException(MethodArgumentNotValidException ex) {
        String detailedMessage = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> (error.getField() + ": " + error.getDefaultMessage()))
                .collect(Collectors.joining(","));
        log.warn("Request body validation failed: {}", detailedMessage, ex);
        var errorDto = getErrorMessageResponse("Validation Error",
                detailedMessage);

        return ResponseEntity.
                status(HttpStatus.BAD_REQUEST)
                .body(errorDto);
    }

    @ExceptionHandler(LocationNotFoundException.class)
    public ResponseEntity<ErrorMessageResponse> handleNoFoundLocationException(
            LocationNotFoundException ex
    ) {
        log.warn("Location not found: {}", ex.getMessage(), ex);
        var errorDto = getErrorMessageResponse("Location search error",
                ex.getMessage());
        return ResponseEntity.
                status(HttpStatus.NOT_FOUND)
                .body(errorDto);
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorMessageResponse> handleNoFoundUserException(
            UserNotFoundException ex
    ) {
        log.warn("User not found: {}", ex.getMessage(), ex);
        var errorDto = getErrorMessageResponse("User search error",
                ex.getMessage());
        return ResponseEntity.
                status(HttpStatus.NOT_FOUND)
                .body(errorDto);
    }

    @ExceptionHandler(UserAlreadyRegisteredException.class)
    public ResponseEntity<ErrorMessageResponse> handleUserAlreadyRegisteredException(
            UserAlreadyRegisteredException ex
    ) {
        log.warn("User registration failed: {}", ex.getMessage(), ex);
        var errorDto = getErrorMessageResponse("User registration error",
                ex.getMessage());
        return ResponseEntity.
                status(HttpStatus.BAD_REQUEST)
                .body(errorDto);
    }

    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<ErrorMessageResponse> handleAuthorizationDeniedException(
            AuthorizationDeniedException ex
    ) {
        log.warn("Access denied: {}", ex.getMessage(), ex);
        var errorDto = getErrorMessageResponse("Forbidden",
                ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(errorDto);
    }

    @ExceptionHandler(DateTimeParseException.class)
    public ResponseEntity<ErrorMessageResponse> handleDateTimeParseException(
            DateTimeParseException ex
    ) {
        log.warn("Date time parsing failed: {}", ex.getMessage(), ex);
        var errorDto = getErrorMessageResponse("Error while parsing date time",
                ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(errorDto);
    }

    @ExceptionHandler(EventNotFoundException.class)
    public ResponseEntity<ErrorMessageResponse> handleEventNotFoundException(
            EventNotFoundException ex
    ) {
        log.warn("Event not found: {}", ex.getMessage(), ex);
        var errorDto = getErrorMessageResponse("Event search error",
                ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(errorDto);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorMessageResponse> handleIllegalArgumentException(
            IllegalArgumentException ex
    ) {
        log.warn("Invalid method argument: {}", ex.getMessage(), ex);
        var errorDto = getErrorMessageResponse("Illegal argument error",
                ex.getMessage());
        return ResponseEntity.
                status(HttpStatus.BAD_REQUEST)
                .body(errorDto);
    }

    @ExceptionHandler(EventPlacesOverflowException.class)
    public ResponseEntity<ErrorMessageResponse> handleEventPlacesOverflowException(
            EventPlacesOverflowException ex
    ) {
        log.warn("Event places overflow: {}", ex.getMessage(), ex);
        var errorDto = getErrorMessageResponse("Error while booking event place",
                ex.getMessage());
        return ResponseEntity.
                status(HttpStatus.BAD_REQUEST)
                .body(errorDto);
    }

    @ExceptionHandler(UserAlreadyRegisteredOnEventException.class)
    public ResponseEntity<ErrorMessageResponse> handleUserAlreadyRegisteredOnEventException(
            UserAlreadyRegisteredOnEventException ex
    ) {
        log.warn("User is already registered on event: {}", ex.getMessage(), ex);
        var errorDto = getErrorMessageResponse("Error while register on event",
                ex.getMessage());
        return ResponseEntity.
                status(HttpStatus.BAD_REQUEST)
                .body(errorDto);
    }

    @ExceptionHandler(EventCountPlacesUpdateException.class)
    public ResponseEntity<ErrorMessageResponse> handleEventCountPlacesUpdateException(
            EventCountPlacesUpdateException ex
    ) {
        log.warn("Event places update validation failed: {}", ex.getMessage(), ex);
        var errorDto = getErrorMessageResponse("Error while matching location and event places count",
                ex.getMessage());
        return ResponseEntity.
                status(HttpStatus.BAD_REQUEST)
                .body(errorDto);
    }

    @ExceptionHandler(UserBookedEventException.class)
    public ResponseEntity<ErrorMessageResponse> handleUserBookedEventException(
            UserBookedEventException ex
    ) {
        log.warn("User booked event operation failed: {}", ex.getMessage(), ex);
        var errorDto = getErrorMessageResponse("Error while cancelling registry request",
                ex.getMessage());
        return ResponseEntity.
                status(HttpStatus.BAD_REQUEST)
                .body(errorDto);
    }

    @ExceptionHandler(NotValidEventStatusException.class)
    public ResponseEntity<ErrorMessageResponse> handleNotValidEventStatusException(
            NotValidEventStatusException ex
    ) {
        log.warn("Invalid event status for requested operation: {}", ex.getMessage(), ex);
        var errorDto = getErrorMessageResponse("Error while checking status for deleting" +
                        " event or registration request",
                ex.getMessage());
        return ResponseEntity.
                status(HttpStatus.BAD_REQUEST)
                .body(errorDto);
    }

    @ExceptionHandler(EventRegistrationNotFoundException.class)
    public ResponseEntity<ErrorMessageResponse> handleEventRegistrationNotFoundException(
            EventRegistrationNotFoundException ex
    ) {
        log.warn("Event registration not found: {}", ex.getMessage(), ex);
        var errorDto = getErrorMessageResponse("Registration search error",
                ex.getMessage());
        return ResponseEntity.
                status(HttpStatus.BAD_REQUEST)
                .body(errorDto);
    }

    @ExceptionHandler(InvalidEventRegistrationStatusException.class)
    public ResponseEntity<ErrorMessageResponse> handleInvalidEventRegistrationStatusException(
            InvalidEventRegistrationStatusException ex
    ) {
        log.warn("Invalid event registration status: {}", ex.getMessage(), ex);
        var errorDto = getErrorMessageResponse("Error while checking registration status",
                ex.getMessage());
        return ResponseEntity.
                status(HttpStatus.BAD_REQUEST)
                .body(errorDto);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorMessageResponse> handleIllegalStateException(
            IllegalStateException ex
    ) {
        log.warn("Illegal application state: {}", ex.getMessage(), ex);
        var errorDto = getErrorMessageResponse("Error while updating event",
                ex.getMessage());
        return ResponseEntity.
                status(HttpStatus.CONFLICT)
                .body(errorDto);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorMessageResponse> handleConstraintViolationException(
            ConstraintViolationException ex
    ) {
        String detailedMessage = ex.getConstraintViolations()
                .stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .collect(Collectors.joining(","));
        log.warn("Constraint validation failed: {}", detailedMessage, ex);
        var errorDto = getErrorMessageResponse("Validation Error", detailedMessage);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(errorDto);
    }

    @ExceptionHandler(LocationCountPlacesException.class)
    public ResponseEntity<ErrorMessageResponse> handleLocationCountPlacesException(
            LocationCountPlacesException ex
    ) {
        log.warn("Location update validation failed: {}", ex.getMessage(), ex);
        var errorDto = getErrorMessageResponse("Error while updating location",
                ex.getMessage());
        return ResponseEntity.
                status(HttpStatus.BAD_REQUEST)
                .body(errorDto);
    }

    private ErrorMessageResponse getErrorMessageResponse(
            String message,
            String detailedMessage
    ) {
        return new ErrorMessageResponse(
                message,
                detailedMessage,
                LocalDateTime.now().toString()
        );
    }
}