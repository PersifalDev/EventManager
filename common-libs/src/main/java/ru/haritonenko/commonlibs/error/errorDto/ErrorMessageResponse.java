package ru.haritonenko.commonlibs.error.errorDto;

public record ErrorMessageResponse(
        String message,
        String detailedMessage,
        String dateTime
) {
}
