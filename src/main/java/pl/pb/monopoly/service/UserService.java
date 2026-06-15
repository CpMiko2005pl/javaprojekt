package pl.pb.monopoly.service;

import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.pb.monopoly.domain.PlayerStatistics;
import pl.pb.monopoly.domain.Role;
import pl.pb.monopoly.domain.User;
import pl.pb.monopoly.dto.RankingEntryDto;
import pl.pb.monopoly.dto.RegistrationForm;
import pl.pb.monopoly.repository.UserRepository;

import java.util.List;

/**
 * Logika biznesowa zwiazana z uzytkownikami: rejestracja, panel admina
 * (doladowanie srodkow, weryfikacja, zmiana roli) oraz ranking dla REST.
 */
@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public boolean usernameTaken(String username) {
        return userRepository.existsByUsername(username);
    }

    public boolean emailTaken(String email) {
        return userRepository.existsByEmail(email);
    }

    /** Rejestracja nowego gracza na podstawie zwalidowanego formularza. */
    @Transactional
    public User register(RegistrationForm form) {
        User user = new User();
        user.setUsername(form.getUsername());
        user.setEmail(form.getEmail());
        user.setFirstName(form.getFirstName());
        user.setLastName(form.getLastName());
        user.setAge(form.getAge());
        user.setPassword(passwordEncoder.encode(form.getPassword()));
        user.setRole(Role.USER);
        user.setCoins(1000); // startowy banknot monet dla nowego konta

        // Kompozycja: kazdy nowy gracz dostaje wlasny rekord statystyk.
        PlayerStatistics stats = new PlayerStatistics();
        stats.setLevel(GameEconomy.levelForElo(stats.getEloPoints())); // poziom spojny z ELO (1000 -> 4)
        user.attachStatistics(stats);

        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public List<User> findAll() {
        return userRepository.findAll();
    }

    @Transactional(readOnly = true)
    public User getById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Nie ma uzytkownika o id " + id));
    }

    /** Panel admina: weryfikacja konta po przeslaniu legitymacji PB. */
    @Transactional
    public void setVerified(Long userId, boolean verified) {
        getById(userId).setVerified(verified);
    }

    /** Panel admina: zarzadzanie rolami uzytkownikow. */
    @Transactional
    public void changeRole(Long userId, Role role) {
        getById(userId).setRole(role);
    }

    @Transactional
    public void delete(Long userId) {
        userRepository.deleteById(userId);
    }

    /** Aktualizacja danych profilu (email, bio, banner, awatar). */
    @Transactional
    public void updateProfile(String username, String newEmail, String bio,
                              String bannerUrl, String avatarUrl, String profileBgUrl) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Nie znaleziono użytkownika"));
        if (!user.getEmail().equalsIgnoreCase(newEmail)
                && userRepository.existsByEmail(newEmail)) {
            throw new IllegalArgumentException("Ten adres e-mail jest już zajęty.");
        }
        if (newEmail != null && !newEmail.isBlank()) {
            user.setEmail(newEmail.strip());
        }
        user.setBio(bio != null ? bio.strip() : null);
        user.setBannerUrl(bannerUrl != null && !bannerUrl.isBlank() ? bannerUrl.strip() : null);
        user.setAvatarUrl(avatarUrl != null && !avatarUrl.isBlank() ? avatarUrl.strip() : null);
        user.setProfileBgUrl(profileBgUrl != null && !profileBgUrl.isBlank() ? profileBgUrl.strip() : null);
    }

    /** Zmiana hasla po weryfikacji biezacego. */
    @Transactional
    public void changePassword(String username, String currentPwd, String newPwd) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Nie znaleziono użytkownika"));
        if (!passwordEncoder.matches(currentPwd, user.getPassword())) {
            throw new IllegalArgumentException("Aktualne hasło jest niepoprawne.");
        }
        if (newPwd == null || newPwd.length() < 6) {
            throw new IllegalArgumentException("Nowe hasło musi mieć co najmniej 6 znaków.");
        }
        user.setPassword(passwordEncoder.encode(newPwd));
    }

    /** Dane do uslugi REST: ranking najlepszych graczy wg punktow ELO. */
    @Transactional(readOnly = true)
    public List<RankingEntryDto> topPlayers() {
        List<User> top = userRepository.findTopPlayers(PageRequest.of(0, 10));
        return java.util.stream.IntStream.range(0, top.size())
                .mapToObj(i -> {
                    User u = top.get(i);
                    var s = u.getStatistics();
                    int elo = s != null ? s.getEloPoints() : 0;
                    int level = s != null ? s.getLevel() : 1;
                    int wins = s != null ? s.getGamesWon() : 0;
                    return new RankingEntryDto(i + 1, u.getUsername(), level, elo, wins);
                })
                .toList();
    }
}
