package pl.pb.monopoly.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Walidacja miedzypolowa (na poziomie klasy): sprawdza, czy haslo i jego
 * powtorzenie sa identyczne. To 6. regula walidacji formularza rejestracji
 * (obok @NotBlank, @Size, @Pattern, @Email, @Min).
 */
@Documented
@Constraint(validatedBy = PasswordMatchesValidator.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface PasswordMatches {

    String message() default "Hasla nie sa identyczne";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
