package ru.haritonenko.eventmanager.error.errorDto;

public record ErrorMessageResponse(
        String message,
        String detailedMessage,
        String dateTime
) {
}
