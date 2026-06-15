package pl.pb.monopoly.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import pl.pb.monopoly.domain.*;
import pl.pb.monopoly.repository.*;
import pl.pb.monopoly.service.GameEconomy;
import pl.pb.monopoly.service.GameService;

import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Wypelnia baze danymi startowymi przy pierwszym uruchomieniu (jesli pusta).
 * Dzieki temu panel gracza (FACEIT), ranking, znajomi i historia meczow sa
 * od razu wypelnione na potrzeby prezentacji.
 *
 * Konta testowe (login / haslo):
 *   admin     / admin123     (ROLE_ADMIN)
 *   moderator / moderator123 (ROLE_MODERATOR)
 *   gracz     / gracz123     (ROLE_USER)
 */
@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner seedData(UserRepository users,
                               MonopolyCardRepository cards,
                               MatchHistoryRepository matches,
                               FriendshipRepository friendships,
                               PasswordEncoder encoder) {
        return args -> {
            if (users.count() == 0) {
                User admin = buildUser("admin", "admin@pb.edu.pl", "Anna", "Adminowska",
                        30, Role.ADMIN, true, encoder, 22, 60, 41);
                User moderator = buildUser("moderator", "mod@pb.edu.pl", "Marek", "Moderacki",
                        28, Role.MODERATOR, true, encoder, 16, 45, 28);
                User gracz = buildUser("gracz", "gracz@pb.edu.pl", "Grzegorz", "Graczewski",
                        21, Role.USER, false, encoder, 12, 38, 23);
                User kuba = buildUser("kuba", "kuba@pb.edu.pl", "Jakub", "Kubicki",
                        23, Role.USER, true, encoder, 9, 30, 14);
                User ola = buildUser("ola", "ola@pb.edu.pl", "Aleksandra", "Olewska",
                        22, Role.USER, true, encoder, 18, 52, 33);

                users.save(admin);
                users.save(moderator);
                users.save(gracz);
                users.save(kuba);
                users.save(ola);

                // Historia meczow dla "gracz" (panel FACEIT)
                seedMatches(matches, gracz);
                seedMatches(matches, ola);

                // Znajomi: gracz <-> moderator (zaakceptowane), gracz <-> kuba (zaakceptowane),
                // admin -> gracz (oczekujace, do akceptacji w panelu)
                friendships.save(new Friendship(gracz, moderator, FriendStatus.ACCEPTED));
                friendships.save(new Friendship(kuba, gracz, FriendStatus.ACCEPTED));
                friendships.save(new Friendship(admin, gracz, FriendStatus.PENDING));
                friendships.save(new Friendship(ola, gracz, FriendStatus.PENDING));
            }

            if (cards.count() == 0) {
                cards.save(buildCard("Stypendium rektora", "Otrzymujesz stypendium naukowe.",
                        CardType.KASA_MIEJSKA, 200));
                cards.save(buildCard("Mandat za rower na deptaku", "Placisz mandat strazy miejskiej.",
                        CardType.SZANSA, -100));
                cards.save(buildCard("Sesja poprawkowa", "Wszyscy gracze placa po 50 PLN do puli.",
                        CardType.WYDARZENIE, -50));
            }
        };
    }

    /**
     * Wypelnia tabele slownikowe przeniesione z kodu: pola planszy (board_tiles),
     * karty planszowe (board_cards), rangi (ranks) i zadania dzienne (daily_tasks).
     * Dane pochodza z dotychczasowych tablic statycznych w GameService/GameEconomy.
     */
    @Bean
    CommandLineRunner seedDictionaries(BoardTileRepository boardTiles,
                                       BoardCardRepository boardCards,
                                       RankRepository ranks,
                                       DailyTaskRepository dailyTasks) {
        return args -> {
            if (boardTiles.count() == 0) {
                for (int pos = 0; pos < GameService.TILES.length; pos++) {
                    int price = GameEconomy.TILE_PRICE[pos];
                    String type = GameEconomy.tileType(pos);
                    Integer rent = "PROPERTY".equals(type) ? GameEconomy.baseRent(pos) : null;
                    boardTiles.save(new BoardTile(
                            pos,
                            GameService.TILES[pos],
                            GameService.TILE_EFFECTS[pos],
                            price > 0 ? price : null,
                            rent,
                            type,
                            null));
                }
            }

            if (boardCards.count() == 0) {
                for (GameService.ChanceCard c : GameService.CHANCE_CARDS) {
                    boardCards.save(new BoardCard("SZANSA", c.title(), c.description(), c.moneyEffect(), null));
                }
            }

            if (ranks.count() == 0) {
                ranks.save(new Rank("Nowicjusz",   0,    799,  "#9ca3af", "rank-novice"));
                ranks.save(new Rank("Brazowy",     800,  999,  "#cd7f32", "rank-bronze"));
                ranks.save(new Rank("Srebrny",     1000, 1199, "#c0c0c0", "rank-silver"));
                ranks.save(new Rank("Zloty",       1200, 1399, "#ffd700", "rank-gold"));
                ranks.save(new Rank("Platynowy",   1400, 1599, "#22d3ee", "rank-platinum"));
                ranks.save(new Rank("Diamentowy",  1600, 1799, "#60a5fa", "rank-diamond"));
                ranks.save(new Rank("Mistrz",      1800, 1999, "#a855f7", "rank-master"));
                ranks.save(new Rank("Legenda PB",  2000, 100000, "#f43f5e", "rank-legend"));
            }

            if (dailyTasks.count() == 0) {
                dailyTasks.save(new DailyTask("Codzienne logowanie",
                        "Zaloguj sie do gry, aby odebrac nagrode.", "COINS", 100, true));
                dailyTasks.save(new DailyTask("Rozegraj partie",
                        "Ukoncz jedna pelna gre.", "COINS", 250, true));
                dailyTasks.save(new DailyTask("Zwycieska passa",
                        "Wygraj jedna gre.", "XP", 150, true));
                dailyTasks.save(new DailyTask("Inwestor",
                        "Kup 3 nieruchomosci w jednej grze.", "COINS", 200, true));
                dailyTasks.save(new DailyTask("Otwieracz skrzyn",
                        "Otworz skrzynke z nagroda.", "LOOTBOX", 1, true));
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
        s.setLevel(GameEconomy.levelForElo(elo)); // poziom spojny z ELO
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

    private MonopolyCard buildCard(String title, String description, CardType type, int effect) {
        MonopolyCard c = new MonopolyCard();
        c.setTitle(title);
        c.setDescription(description);
        c.setType(type);
        c.setMoneyEffect(effect);
        return c;
    }
}
