package pl.pb.monopoly.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import pl.pb.monopoly.domain.*;
import pl.pb.monopoly.repository.*;
import pl.pb.monopoly.service.GameEconomy;

import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner seedData(UserRepository users,
                               MatchHistoryRepository matches,
                               FriendshipRepository friendships,
                               ProfileCommentRepository comments,
                               PasswordEncoder encoder) {
        return args -> {
            if (users.count() == 0) {
                User admin = buildUser("admin", "admin@pb.edu.pl", "Anna", "Adminowska",
                        30, Role.ADMIN, true, encoder, 22, 60, 41);
                User moderator = buildUser("moderator", "mod@pb.edu.pl", "Marek", "Moderacki",
                        28, Role.MODERATOR, true, encoder, 16, 45, 28);
                User gracz = buildUser("gracz", "gracz@pb.edu.pl", "Grzegorz", "Graczewski",
                        21, Role.USER, false, encoder, 12, 38, 23);

                users.save(admin);
                users.save(moderator);
                users.save(gracz);

                seedMatches(matches, gracz);

                friendships.save(new Friendship(gracz, moderator, FriendStatus.ACCEPTED));
                friendships.save(new Friendship(admin, gracz, FriendStatus.PENDING));

                comments.save(new ProfileComment(moderator, gracz, "Pamietaj o zasadach kultury na czacie!"));
            }
        };
    }

    private User buildUser(String username, String email, String firstName, String lastName,
                           int age, Role role, boolean verified, PasswordEncoder encoder,
                           int level, int games, int wins) {
        User u = new User();
        u.setUsername(username);
        u.setEmail(email);
        u.setFirstName(firstName);
        u.setLastName(lastName);
        u.setAge(age);
        u.setRole(role);
        u.setVerified(verified);
        u.setCoins(1000 + wins * 50);
        u.setPassword(encoder.encode(username + "123"));

        PlayerStatistics s = new PlayerStatistics();
        int elo = 800 + level * 90 + wins * 5;
        s.setEloPoints(elo);
        s.setLevel(GameEconomy.levelForElo(elo));
        s.setGamesPlayed(games);
        s.setGamesWon(wins);
        s.setWinStreak(ThreadLocalRandom.current().nextInt(0, 5));
        s.setDailyStreak(ThreadLocalRandom.current().nextInt(1, 7));
        u.attachStatistics(s);
        return u;
    }

    private void seedMatches(MatchHistoryRepository matches, User user) {
        for (int i = 0; i < 8; i++) {
            boolean won = ThreadLocalRandom.current().nextBoolean();
            MatchHistory m = new MatchHistory();
            m.setUser(user);
            m.setPlayedAt(LocalDateTime.now().minusDays(i).minusHours(ThreadLocalRandom.current().nextInt(0, 12)));
            m.setBoardName("Kampus PB");
            m.setWon(won);
            int playersCount = ThreadLocalRandom.current().nextInt(2, 5);
            m.setPlayersCount(playersCount);
            m.setPlacement(won ? 1 : ThreadLocalRandom.current().nextInt(2, playersCount + 1));
            m.setFinalCash(won ? ThreadLocalRandom.current().nextInt(2000, 6000)
                               : ThreadLocalRandom.current().nextInt(0, 800));
            m.setDurationMinutes(ThreadLocalRandom.current().nextInt(18, 65));
            m.setEloChange(won ? ThreadLocalRandom.current().nextInt(18, 32)
                               : -ThreadLocalRandom.current().nextInt(12, 26));
            matches.save(m);
        }
    }
}
