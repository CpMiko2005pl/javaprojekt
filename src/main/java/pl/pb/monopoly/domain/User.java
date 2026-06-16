package pl.pb.monopoly.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.time.LocalDateTime;

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

    @Min(value = 0, message = "Liczba monet nie moze byc ujemna")
    @Column(nullable = false)
    private int coins = 1000;

    @Column(nullable = false)
    private boolean verified = false;

    @Column(name = "verification_doc_url", length = 255)
    private String verificationDocUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role = Role.USER;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "avatar_url", length = 512)
    private String avatarUrl;

    @Column(name = "banner_url", length = 512)
    private String bannerUrl;

    @Column(name = "profile_bg_url", length = 512)
    private String profileBgUrl;

    @Column(name = "bio", length = 200)
    private String bio;

    @Column(nullable = false)
    private boolean suspended = false;

    @Column(name = "suspended_until")
    private LocalDateTime suspendedUntil;

    @Column(name = "suspension_reason", length = 500)
    private String suspensionReason;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private PlayerStatistics statistics;

    public User() {
    }

    public void attachStatistics(PlayerStatistics stats) {
        stats.setUser(this);
        this.statistics = stats;
    }

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

    public int getCoins() {
        return coins;
    }

    public void setCoins(int coins) {
        this.coins = Math.max(0, coins);
    }

    public void addCoins(int amount) {
        this.coins = Math.max(0, this.coins + amount);
    }

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    public String getVerificationDocUrl() {
        return verificationDocUrl;
    }

    public void setVerificationDocUrl(String verificationDocUrl) {
        this.verificationDocUrl = verificationDocUrl;
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

    public String getProfileBgUrl() {
        return profileBgUrl;
    }

    public void setProfileBgUrl(String profileBgUrl) {
        this.profileBgUrl = profileBgUrl;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public boolean isSuspended() {
        return suspended;
    }

    public void setSuspended(boolean suspended) {
        this.suspended = suspended;
    }

    public LocalDateTime getSuspendedUntil() {
        return suspendedUntil;
    }

    public void setSuspendedUntil(LocalDateTime suspendedUntil) {
        this.suspendedUntil = suspendedUntil;
    }

    public String getSuspensionReason() {
        return suspensionReason;
    }

    public void setSuspensionReason(String suspensionReason) {
        this.suspensionReason = suspensionReason;
    }
}
