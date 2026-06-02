package pl.pb.monopoly.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.pb.monopoly.domain.PlayerStatistics;
import pl.pb.monopoly.domain.User;
import pl.pb.monopoly.dto.WheelResultDto;
import pl.pb.monopoly.repository.UserRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Codzienne Kolo Fortuny Stypendialnego - gracz raz dziennie losuje ulatwienie
 * do gry. Powiazane z Daily Streak (seria dni logowania/losowania).
 */
@Service
public class WheelService {

    /** Pola kola (kolejnosc istotna - indeks uzywany do animacji na froncie). */
    public static final List<String> REWARDS = List.of(
            "+300 siana na start gry",
            "Karta wyjscia z Akademika",
            "Tarcza przed bankructwem (1 mecz)",
            "Dodatkowy rzut kostka",
            "Znizka 50% na czynsz (1 tura)",
            "Podwojne stypendium za START",
            "Boost +150 ELO po wygranej",
            "Losowa nieruchomosc gratis"
    );

    private final UserRepository userRepository;

    public WheelService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /** Liczba pol kola - przydatne dla widoku. */
    public int segmentCount() {
        return REWARDS.size();
    }

    /** Czy gracz moze dzis jeszcze zakrecic. */
    @Transactional(readOnly = true)
    public boolean canSpinToday(String username) {
        User user = userRepository.findByUsername(username).orElseThrow();
        PlayerStatistics s = user.getStatistics();
        return s == null || !LocalDate.now().equals(s.getLastSpinDate());
    }

    @Transactional
    public WheelResultDto spin(String username) {
        User user = userRepository.findByUsername(username).orElseThrow();
        PlayerStatistics s = user.getStatistics();
        if (s == null) {
            s = new PlayerStatistics();
            user.attachStatistics(s);
        }

        LocalDate today = LocalDate.now();
        if (today.equals(s.getLastSpinDate())) {
            return new WheelResultDto(false, -1, s.getLastReward(), s.getDailyStreak(),
                    "Dzis juz losowales. Wroc jutro po kolejna nagrode!");
        }

        // Aktualizacja serii dni (streak): +1 jesli wczoraj, inaczej reset do 1.
        if (today.minusDays(1).equals(s.getLastSpinDate())) {
            s.setDailyStreak(s.getDailyStreak() + 1);
        } else {
            s.setDailyStreak(1);
        }

        int idx = ThreadLocalRandom.current().nextInt(REWARDS.size());
        String reward = REWARDS.get(idx);
        s.setLastSpinDate(today);
        s.setLastReward(reward);

        return new WheelResultDto(true, idx, reward, s.getDailyStreak(),
                "Gratulacje! Wylosowales: " + reward);
    }
}
