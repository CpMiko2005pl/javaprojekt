package pl.pb.monopoly.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = MinimumAgeValidator.class)
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface MinimumAge {

    int value();

    String message() default "Wymagany wiek to co najmniej {value} lat";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
