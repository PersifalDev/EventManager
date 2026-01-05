package ru.haritonenko.eventmanager.event.domain.custom.validation.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.stereotype.Component;
import ru.haritonenko.eventmanager.event.domain.custom.validation.annotation.NotPastDateTime;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import static java.util.Objects.isNull;

@Component
public class NotPastDateTimeValidator implements ConstraintValidator<NotPastDateTime, String> {

    private DateTimeFormatter formatter;
    private boolean allowBlank;

    @Override
    public void initialize(NotPastDateTime annotation) {
        this.formatter = DateTimeFormatter.ofPattern(annotation.pattern());
        this.allowBlank = annotation.allowBlank();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (isNull(value) || value.isBlank()) {
            return allowBlank;
        }
        final LocalDateTime dateTime;
        try {
            dateTime = LocalDateTime.parse(value, formatter);
        } catch (DateTimeParseException e) {
            return false;
        }
        return !dateTime.isBefore(LocalDateTime.now());
    }
}
