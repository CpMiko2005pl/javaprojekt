package pl.pb.monopoly.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import pl.pb.monopoly.domain.AchievementType;
import pl.pb.monopoly.domain.PlayerStatistics;
import pl.pb.monopoly.domain.User;
import pl.pb.monopoly.repository.AchievementRepository;
import pl.pb.monopoly.repository.UserRepository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("h2")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AchievementServiceTest {

    @Autowired
    AchievementRepository achRepo;
    @Autowired
    UserRepository users;

    private User newUser(int gamesPlayed, int gamesWon, int winStreak) {
        User u = new User();
        u.setUsername("achtest");
        u.setEmail("achtest@pb.edu.pl");
        u.setAge(20);
        u.setPassword("x");
        u.setCoins(0);
        PlayerStatistics s = new PlayerStatistics();
        s.setGamesPlayed(gamesPlayed);
        s.setGamesWon(gamesWon);
        s.setWinStreak(winStreak);
        u.attachStatistics(s);
        return users.saveAndFlush(u);
    }

    @Test
    void firstGameAndWinUnlockAndGrantCoins() {
        AchievementService svc = new AchievementService(achRepo, users);
        User u = newUser(1, 1, 1);

        List<AchievementType> newly = svc.checkAndAward(u.getId());

        assertTrue(newly.contains(AchievementType.FIRST_GAME));
        assertTrue(newly.contains(AchievementType.FIRST_WIN));
        assertFalse(newly.contains(AchievementType.WINS_5));
        assertEquals(AchievementType.FIRST_GAME.coinReward + AchievementType.FIRST_WIN.coinReward,
                users.findById(u.getId()).orElseThrow().getCoins());
        assertEquals(2, achRepo.findByUserIdOrderByUnlockedAtDesc(u.getId()).size());
    }

    @Test
    void awardingIsIdempotent() {
        AchievementService svc = new AchievementService(achRepo, users);
        User u = newUser(1, 1, 0);

        svc.checkAndAward(u.getId());
        int coinsAfterFirst = users.findById(u.getId()).orElseThrow().getCoins();
        List<AchievementType> again = svc.checkAndAward(u.getId());

        assertTrue(again.isEmpty());
        assertEquals(coinsAfterFirst, users.findById(u.getId()).orElseThrow().getCoins());
    }
}
