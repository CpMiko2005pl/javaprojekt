package pl.pb.monopoly.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import pl.pb.monopoly.dto.RegistrationForm;

/**
 * Logika walidacji adnotacji {@link PasswordMatches}.
 * Komunikat o bledzie wiazemy z polem "confirmPassword", aby pojawil sie
 * we wlasciwym miejscu formularza.
 */
public class PasswordMatchesValidator implements ConstraintValidator<PasswordMatches, RegistrationForm> {

    @Override
    public boolean isValid(RegistrationForm form, ConstraintValidatorContext context) {
        if (form.getPassword() == null) {
            return true; // puste haslo obsluguja inne adnotacje (@NotBlank/@Size)
        }
        boolean matches = form.getPassword().equals(form.getConfirmPassword());
        if (!matches) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                    .addPropertyNode("confirmPassword")
                    .addConstraintViolation();
        }
        return matches;
    }
}
