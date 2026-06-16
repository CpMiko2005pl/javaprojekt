package pl.pb.monopoly.dto;

import jakarta.validation.constraints.*;
import pl.pb.monopoly.validation.MinimumAge;

import java.time.LocalDate;

public class AdminUserEditForm {

    @NotBlank(message = "E-mail jest wymagany")
    @Email(message = "Niepoprawny adres e-mail")
    private String email;

    @Size(min = 3, max = 20, message = "Imie: od 3 do 20 znakow")
    private String firstName;

    @Size(min = 3, max = 50, message = "Nazwisko: od 3 do 50 znakow")
    private String lastName;

    @NotNull(message = "Data urodzenia jest wymagana")
    @Past(message = "Data urodzenia nie moze byc w przyszlosci")
    @MinimumAge(18)
    private LocalDate dateOfBirth;

    @Min(value = 0, message = "Liczba monet nie moze byc ujemna")
    private int coins;

    @Size(max = 200, message = "Opis moze miec maksymalnie 200 znakow")
    private String bio;

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }

    public int getCoins() { return coins; }
    public void setCoins(int coins) { this.coins = coins; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }
}
