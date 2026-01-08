package ru.haritonenko.eventmanager.error.handler;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.haritonenko.eventmanager.event.domain.exception.EventCountPlacesException;
import ru.haritonenko.eventmanager.event.domain.exception.EventInvalidStatusException;
import ru.haritonenko.eventmanager.event.domain.exception.EventNotFoundException;
import ru.haritonenko.eventmanager.event.domain.exception.EventPlacesOverflowException;
import ru.haritonenko.eventmanager.event.registration.exception.EventRegistrationNotFoundException;
import ru.haritonenko.eventmanager.event.registration.exception.InvalidEventRegistrationStatusException;
import ru.haritonenko.eventmanager.location.domain.exception.LocationCountPlacesException;
import ru.haritonenko.eventmanager.user.domain.exception.UserAlreadyRegisteredOnEventException;
import ru.haritonenko.eventmanager.location.domain.exception.LocationNotFoundException;
import ru.haritonenko.eventmanager.user.domain.exception.UserAlreadyRegisteredException;
import ru.haritonenko.eventmanager.user.domain.exception.UserBookedEventException;
import ru.haritonenko.eventmanager.user.domain.exception.UserNotFoundException;
import ru.haritonenko.eventmanager.error.errorDto.ErrorMessageResponse;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorMessageResponse> handleValidationException(MethodArgumentNotValidException ex) {
        log.warn("Got validation exception", ex);
        String detailedMessage = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> (error.getField() + ": " + error.getDefaultMessage()))
                .collect(Collectors.joining(","));
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
        log.warn("Got NoFoundLocationException", ex);
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
        log.warn("Got NoFoundUserException", ex);
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
        log.warn("Got AlreadyRegisteredUserException", ex);
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
        log.error("Got AuthorizationDeniedException", ex);
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
        log.warn("Got DateTimeParseException", ex);
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
        log.warn("Got EventNotFoundException", ex);
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
        log.warn("Got IllegalArgumentException", ex);
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
        log.warn("Got EventPlacesOverflowException", ex);
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
        log.warn("Got UserAlreadyRegisteredOnEventException", ex);
        var errorDto = getErrorMessageResponse("Error while register on event",
                ex.getMessage());
        return ResponseEntity.
                status(HttpStatus.BAD_REQUEST)
                .body(errorDto);
    }

    @ExceptionHandler(EventCountPlacesException.class)
    public ResponseEntity<ErrorMessageResponse> handleEventCountPlacesException(
            EventCountPlacesException ex
    ) {
        log.warn("Got EventCountPlacesException", ex);
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
        log.warn("Got UserBookedEventException", ex);
        var errorDto = getErrorMessageResponse("Error while cancelling registry request",
                ex.getMessage());
        return ResponseEntity.
                status(HttpStatus.BAD_REQUEST)
                .body(errorDto);
    }

    @ExceptionHandler(EventInvalidStatusException.class)
    public ResponseEntity<ErrorMessageResponse> handleEventInvalidStatusException(
            EventInvalidStatusException ex
    ) {
        log.warn("Got EventInvalidStatusException", ex);
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
        log.warn("Got EventRegistrationNotFoundException", ex);
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
        log.warn("Got InvalidEventRegistrationStatusException", ex);
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
        log.warn("Got IllegalStateException", ex);
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
        log.warn("Got ConstraintViolationException", ex);

        String detailedMessage = ex.getConstraintViolations()
                .stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .collect(Collectors.joining(","));

        var errorDto = getErrorMessageResponse("Validation Error", detailedMessage);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(errorDto);
    }

    @ExceptionHandler(LocationCountPlacesException.class)
    public ResponseEntity<ErrorMessageResponse> handleLocationCountPlacesException(
            LocationCountPlacesException ex
    ) {
        log.warn("Got LocationCountPlacesException", ex);
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
