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

import java.math.BigDecimal;
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

        // Kompozycja: kazdy nowy gracz dostaje wlasny rekord statystyk.
        user.attachStatistics(new PlayerStatistics());

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

    /** Panel admina/moderatora: doladowanie srodkow (PLN) na konto gracza. */
    @Transactional
    public void topUpBalance(Long userId, BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Kwota doladowania musi byc dodatnia");
        }
        User user = getById(userId);
        user.setBalance(user.getBalance().add(amount));
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
