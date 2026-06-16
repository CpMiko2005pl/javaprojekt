package pl.pb.monopoly.service;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.pb.monopoly.domain.PlayerStatistics;
import pl.pb.monopoly.domain.Role;
import pl.pb.monopoly.domain.User;
import pl.pb.monopoly.dto.AdminUserEditForm;
import pl.pb.monopoly.dto.RankingEntryDto;
import pl.pb.monopoly.dto.RegistrationForm;
import pl.pb.monopoly.repository.UserRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Logika biznesowa zwiazana z uzytkownikami: rejestracja, panel admina
 * (doladowanie srodkow, weryfikacja, zmiana roli) oraz ranking dla REST.
 */
@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AvatarFetchService avatarFetchService;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       AvatarFetchService avatarFetchService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.avatarFetchService = avatarFetchService;
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

        // Klient REST: pobierz awatar z DiceBear, jesli gracz nie ustawil wlasnego.
        if (user.getAvatarUrl() == null || user.getAvatarUrl().isBlank()) {
            String avatar = avatarFetchService.fetchAvatarUrl(user.getUsername());
            if (avatar != null) {
                user.setAvatarUrl(avatar);
            }
        }

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

    /** Dozwolone kryteria sortowania listy admina (whitelist — ochrona przed wstrzyknieciem). */
    private static final java.util.Set<String> SORTABLE = java.util.Set.of("username", "createdAt", "coins");

    /**
     * Panel admina: lista uzytkownikow z filtrami (data rejestracji od, rola) i
     * sortowaniem (username/createdAt/coins, rosnaco/malejaco). Wspolna dla MVC i REST.
     */
    @Transactional(readOnly = true)
    public List<User> findForAdmin(String sort, String dir, LocalDate dateFrom, Role role) {
        String property = (sort != null && SORTABLE.contains(sort)) ? sort : "username";
        Sort.Direction direction = "desc".equalsIgnoreCase(dir) ? Sort.Direction.DESC : Sort.Direction.ASC;
        LocalDateTime from = dateFrom != null ? dateFrom.atStartOfDay() : null;
        return userRepository.findForAdmin(role, from, Sort.by(direction, property));
    }

    /** Panel admina: edycja danych biezacych uzytkownika (bez hasla i roli). */
    @Transactional
    public void updateByAdmin(Long id, AdminUserEditForm form) {
        User user = getById(id);
        String newEmail = form.getEmail() != null ? form.getEmail().strip() : null;
        if (newEmail != null && !user.getEmail().equalsIgnoreCase(newEmail)
                && userRepository.existsByEmail(newEmail)) {
            throw new IllegalArgumentException("Ten adres e-mail jest juz zajety.");
        }
        if (newEmail != null && !newEmail.isBlank()) {
            user.setEmail(newEmail);
        }
        user.setFirstName(form.getFirstName());
        user.setLastName(form.getLastName());
        user.setAge(form.getAge());
        user.setCoins(form.getCoins());
        user.setBio(form.getBio() != null && !form.getBio().isBlank() ? form.getBio().strip() : null);
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
