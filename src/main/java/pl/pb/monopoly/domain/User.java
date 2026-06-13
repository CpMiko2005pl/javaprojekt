package pl.pb.monopoly.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Glowna encja domeny - gracz/student PB.
 *
 * Relacje KOMPOZYCJI (cascade = ALL + orphanRemoval = true) - obiekty potomne
 * nie istnieja bez wlasciciela i sa usuwane razem z nim:
 *   - User 1--1 PlayerStatistics  (statystyki naleza scisle do uzytkownika)
 *   - User 1--* Achievement       (osiagniecia gracza)
 *   - User 1--* GameLog           (historia/logi rozgrywek gracza)
 *
 * Pola roznych typow: String, int (posrednio), BigDecimal, boolean, LocalDateTime, enum.
 * Ograniczenia wartosci nalozone adnotacjami walidacji ORAZ ograniczeniami kolumn.
 */
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Login jest wymagany")
    @Size(min = 3, max = 20, message = "Login musi miec od 3 do 20 znakow")
    @Pattern(regexp = "^[a-z0-9]+$", message = "Login: tylko male litery i cyfry")
    @Column(nullable = false, unique = true, length = 20)
    private String username;

    /** Haslo przechowywane WYLACZNIE jako hash BCrypt. */
    @NotBlank
    @Column(nullable = false)
    private String password;

    @NotBlank
    @Email(message = "Niepoprawny adres e-mail")
    @Column(nullable = false, unique = true)
    private String email;

    @Size(min = 3, max = 20)
    @Column(name = "first_name", length = 20)
    private String firstName;

    @Size(min = 3, max = 50)
    @Column(name = "last_name", length = 50)
    private String lastName;

    @Min(value = 18, message = "Wymagany wiek to co najmniej 18 lat")
    @Max(value = 120)
    @Column(nullable = false)
    private int age;

    /** Saldo gracza w PLN. Domyslnie 1500 PLN (startowy budzet jak w Monopoly). */
    @NotNull
    @DecimalMin(value = "0.0", message = "Saldo nie moze byc ujemne")
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal balance = new BigDecimal("1500.00");

    /** Czy konto zweryfikowano (po przeslaniu skanu legitymacji PB). */
    @Column(nullable = false)
    private boolean verified = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role = Role.USER;

    /** Pole typu Date (wymagane). */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    /** Adres URL niestandardowego awatara (opcjonalne). */
    @Column(name = "avatar_url", length = 512)
    private String avatarUrl;

    /** Adres URL banera profilowego (opcjonalne). */
    @Column(name = "banner_url", length = 512)
    private String bannerUrl;

    /** Krotki opis gracza widoczny na profilu publicznym. */
    @Column(name = "bio", length = 200)
    private String bio;

    // --- KOMPOZYCJA ---

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private PlayerStatistics statistics;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Achievement> achievements = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GameLog> gameLogs = new ArrayList<>();

    public User() {
    }

    /** Wygodne podlaczenie statystyk z zachowaniem obustronnej relacji. */
    public void attachStatistics(PlayerStatistics stats) {
        stats.setUser(this);
        this.statistics = stats;
    }

    // --- Gettery / settery ---

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
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

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public PlayerStatistics getStatistics() {
        return statistics;
    }

    public void setStatistics(PlayerStatistics statistics) {
        this.statistics = statistics;
    }

    public List<Achievement> getAchievements() {
        return achievements;
    }

    public void setAchievements(List<Achievement> achievements) {
        this.achievements = achievements;
    }

    public List<GameLog> getGameLogs() {
        return gameLogs;
    }

    public void setGameLogs(List<GameLog> gameLogs) {
        this.gameLogs = gameLogs;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getBannerUrl() {
        return bannerUrl;
    }

    public void setBannerUrl(String bannerUrl) {
        this.bannerUrl = bannerUrl;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }
}
