package ru.haritonenko.eventmanager.event.domain.custom.validation.annotation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import ru.haritonenko.eventmanager.event.domain.custom.validation.validator.NotPastDateTimeValidator;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = NotPastDateTimeValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface NotPastDateTime {

    String message() default "Date/time must be now or in the future";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    String pattern() default "yyyy-MM-dd'T'HH:mm:ss";

    boolean allowBlank() default false;

}
