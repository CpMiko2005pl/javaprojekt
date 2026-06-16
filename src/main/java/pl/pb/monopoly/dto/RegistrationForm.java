package pl.pb.monopoly.dto;

import jakarta.validation.constraints.*;
import pl.pb.monopoly.validation.PasswordMatches;

@PasswordMatches
public class RegistrationForm {

    @NotBlank(message = "Login jest wymagany")
    @Size(min = 3, max = 20, message = "Login musi miec od 3 do 20 znakow")
    @Pattern(regexp = "^[a-z0-9]+$", message = "Login: dozwolone tylko male litery i cyfry")
    private String username;

    @NotBlank(message = "E-mail jest wymagany")
    @Email(message = "Podaj poprawny adres e-mail")
    private String email;

    @Size(min = 3, max = 20, message = "Imie musi miec od 3 do 20 znakow")
    @Pattern(regexp = "^[A-ZĄĆĘŁŃÓŚŹŻ][a-ząćęłńóśźż]+$",
            message = "Imie musi zaczynac sie wielka litera")
    private String firstName;

    @Size(min = 3, max = 50, message = "Nazwisko musi miec od 3 do 50 znakow")
    @Pattern(regexp = "^[A-ZĄĆĘŁŃÓŚŹŻ][a-ząćęłńóśźż-]+$",
            message = "Nazwisko musi zaczynac sie wielka litera")
    private String lastName;

    @Min(value = 18, message = "Do gry wymagany jest wiek co najmniej 18 lat")
    @Max(value = 120, message = "Niepoprawny wiek")
    private int age;

    @NotBlank(message = "Haslo jest wymagane")
    @Size(min = 5, max = 100, message = "Haslo musi miec co najmniej 5 znakow")
    private String password;

    @NotBlank(message = "Powtorz haslo")
    private String confirmPassword;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }
}
