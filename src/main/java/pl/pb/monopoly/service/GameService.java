package pl.pb.monopoly.service;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.pb.monopoly.domain.*;
import pl.pb.monopoly.dto.GamePlayerDto;
import pl.pb.monopoly.dto.GameStateDto;
import pl.pb.monopoly.repository.GameSessionRepository;
import pl.pb.monopoly.repository.UserRepository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Silnik rozgrywki Kampus PB: ruch po planszy, Dziekanat (wiezienie),
 * podatki, transfery siana. Stan synchronizowany przez WebSocket.
 */
@Service
public class GameService {

    /** 40 pol — Politechnika Bialostocka + Bialystok (nazwy + efekty). */
    public static final String[] TILES = {
            "START — Inauguracja semestru",
            "Akademik Bratniak",
            "Kasa Miejska — Stypendium",
            "Akademik Sloneczny",
            "Oplata za warunki",
            "Dworzec PKP Bialystok",
            "Wydzial Elektryczny",
            "Karta Szansy — USOS",
            "Wydzial Mechaniczny",
            "Wydzial Budownictwa",
            "DZIEKANAT (Wiezienie)",
            "Klub Studencki Gwint",
            "Wydzial Chemiczny",
            "Stolowka Studencka",
            "Biblioteka Politechniki",
            "Dworzec PKS",
            "Wydzial Informatyki W-8",
            "Kasa Miejska — Stypendium",
            "Wydzial Zarzadzania",
            "Wydzial Architektury",
            "Parking Studencki",
            "Rynek Kosciuszki",
            "Karta Szansy — Sesja",
            "Galeria Alfa",
            "Spodek — Rektorat PB",
            "Dworzec Fabryczny",
            "Palac Branickich",
            "Opera Podlaska",
            "Wodociagi PB",
            "Hala Sportowa",
            "Idz do Dziekanatu",
            "Las Zwierzyniecki",
            "Galeria Biala",
            "Kasa Miejska — Stypendium",
            "Gmach Glowny Politechniki",
            "Lotnisko Krywlany",
            "Karta Szansy — Kolokwium",
            "Aula Magna",
            "Oplata luksusowa",
            "Meta — Kampus PB"
    };

    public static final String[] TILE_EFFECTS = {
            "Przejscie: +200 PLN",
            "Nieruchomosc — 140 PLN",
            "Stypendium +200 PLN",
            "Nieruchomosc — 140 PLN",
            "Oplata — 200 PLN",
            "Dworzec — czynsz x25",
            "Wydzial — 280 PLN / czynsz 24",
            "Losuj karte zdarzenia",
            "Wydzial — 260 PLN / czynsz 22",
            "Wydzial — 260 PLN / czynsz 22",
            "WIEZIENIE: lapowka 100 PLN",
            "Nieruchomosc — 180 PLN",
            "Wydzial — 240 PLN / czynsz 20",
            "Nieruchomosc — 160 PLN",
            "Biblioteka — 220 PLN",
            "Dworzec — czynsz x25",
            "W-8 Informatyki — 320 PLN",
            "Stypendium +200 PLN",
            "Wydzial — 240 PLN",
            "Wydzial — 220 PLN",
            "Postoj — bez oplaty",
            "Nieruchomosc — 300 PLN",
            "Losuj karte zdarzenia",
            "Nieruchomosc — 260 PLN",
            "Rektorat — 350 PLN",
            "Dworzec — czynsz x25",
            "Nieruchomosc — 280 PLN",
            "Nieruchomosc — 260 PLN",
            "Urzadzenia — czynsz x4",
            "Nieruchomosc — 240 PLN",
            "Idz na Dziekanat (poz. 10)",
            "Nieruchomosc — 200 PLN",
            "Nieruchomosc — 180 PLN",
            "Stypendium +200 PLN",
            "Gmach Glowny — 400 PLN",
            "Dworzec — czynsz x25",
            "Losuj karte zdarzenia",
            "Aula — 380 PLN",
            "Oplata — 100 PLN",
            "Bonus okragowy +200 PLN"
    };

    /** Indeksy pol specjalnych. */
    public static final int POS_START = 0;
    public static final int POS_JAIL = 10;
    public static final int POS_GO_TO_JAIL = 30;

    private static final String[] COLORS = {
            "#38bdf8", "#a855f7", "#10b981", "#f59e0b", "#f43f5e", "#6366f1"
    };

    private final GameSessionRepository sessionRepository;
    private final UserRepository userRepository;
    private final GameSyncService gameSyncService;

    public GameService(GameSessionRepository sessionRepository,
                       UserRepository userRepository,
                       @Lazy GameSyncService gameSyncService) {
        this.sessionRepository = sessionRepository;
        this.userRepository = userRepository;
        this.gameSyncService = gameSyncService;
    }

    public static List<String> tileNamesList() {
        return List.of(TILES);
    }

    public static List<String> tileEffectsList() {
        return List.of(TILE_EFFECTS);
    }

    private User user(String username) {
        return userRepository.findByUsername(username).orElseThrow();
    }

    @Transactional(readOnly = true)
    public List<GameSession> myActiveSessions(String username) {
        return sessionRepository.findActiveByUser(username);
    }

    @Transactional
    public GameSession createGame(String creatorUsername, String roomName,
                                  List<Long> friendIds, int bots) {
        GameSession session = new GameSession();
        session.setName(roomName == null || roomName.isBlank() ? "Pokoj " + creatorUsername : roomName.trim());
        session.setCode(generateCode());
        session.setStatus(GameStatus.ACTIVE);

        User creator = user(creatorUsername);
        GamePlayer me = new GamePlayer(creator.getUsername(), COLORS[0]);
        me.setUser(creator);
        session.addPlayer(me);

        if (friendIds != null) {
            for (Long fid : friendIds) {
                userRepository.findById(fid).ifPresent(friend -> {
                    if (session.getPlayers().stream().noneMatch(p ->
                            p.getUser() != null && p.getUser().getId().equals(friend.getId()))) {
                        GamePlayer gp = new GamePlayer(friend.getUsername(),
                                COLORS[session.getPlayers().size() % COLORS.length]);
                        gp.setUser(friend);
                        session.addPlayer(gp);
                    }
                });
            }
        }

        int botsToAdd = Math.max(bots, session.getPlayers().size() < 2 ? 1 : 0);
        for (int i = 0; i < botsToAdd; i++) {
            session.addPlayer(new GamePlayer("Bot " + (i + 1),
                    COLORS[session.getPlayers().size() % COLORS.length]));
        }

        GameSession saved = sessionRepository.save(session);
        publishPublic(saved.getId(), null, null, null, null, null, null);
        return saved;
    }

    @Transactional
    public GameSession joinByCode(String code, String username) {
        GameSession session = sessionRepository.findByCode(code.trim().toUpperCase())
                .orElseThrow(() -> new IllegalArgumentException("Nie ma pokoju o kodzie " + code));
        User u = user(username);
        boolean already = session.getPlayers().stream()
                .anyMatch(p -> p.getUser() != null && p.getUser().getId().equals(u.getId()));
        if (!already) {
            GamePlayer gp = new GamePlayer(u.getUsername(), COLORS[session.getPlayers().size() % COLORS.length]);
            gp.setUser(u);
            session.addPlayer(gp);
            sessionRepository.save(session);
            publishPublic(session.getId(), null, null, u.getUsername() + " dolaczyl do gry!", null, null, null);
        }
        return session;
    }

    @Transactional(readOnly = true)
    public GameSession getSession(Long id) {
        return sessionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Nie ma sesji o id " + id));
    }

    @Transactional(readOnly = true)
    public GameStateDto getState(Long sessionId, String username) {
        return toState(getSession(sessionId), username, null, null, null, null, null, null);
    }

    /** Rzut kostka — tylko gracz z aktualna tura. */
    @Transactional
    public GameStateDto roll(Long sessionId, String username) {
        GameSession session = getSession(sessionId);
        requireParticipant(session, username);
        List<GamePlayer> players = session.getPlayers();
        if (players.isEmpty()) {
            return toState(session, username, null, null, "Brak graczy.", null, null, null);
        }

        GamePlayer current = players.get(session.getCurrentTurn() % players.size());
        if (current.getUser() == null || !current.getUser().getUsername().equals(username)) {
            throw new IllegalArgumentException("To nie Twoja tura! Czekaj na ruch innego gracza.");
        }
        if (current.isBankrupt()) {
            throw new IllegalArgumentException("Jestes bankrutem i nie mozesz grac.");
        }

        int d1 = ThreadLocalRandom.current().nextInt(1, 7);
        int d2 = ThreadLocalRandom.current().nextInt(1, 7);
        int steps = d1 + d2;

        int oldPos = current.getPosition();
        int newPos = (oldPos + steps) % 40;
        StringBuilder msg = new StringBuilder();
        msg.append(current.getDisplayName()).append(" rzuca ")
           .append(d1).append("+").append(d2).append("=").append(steps).append(". ");

        if (oldPos + steps >= 40) {
            current.setCash(current.getCash() + 200);
            msg.append("Przejscie przez START (+200 PLN). ");
        }

        if (newPos == POS_GO_TO_JAIL) {
            newPos = POS_JAIL;
            msg.append("Trafia na pole „Idz do Dziekanatu” — laduje w WIEZIENIU! ");
        }

        current.setPosition(newPos);
        msg.append("Staje na: ").append(TILES[newPos]).append(". ");
        applyTileEffect(current, newPos, msg);

        advanceTurn(session);
        sessionRepository.save(session);

        Long movedId = current.getId();
        publishPublic(sessionId, d1, d2, msg.toString(), movedId, oldPos, newPos);
        return toState(session, username, d1, d2, msg.toString(), movedId, oldPos, newPos);
    }

    @Transactional
    public GameStateDto transfer(Long sessionId, String username, Long toPlayerId, int amount) {
        GameSession session = getSession(sessionId);
        if (amount <= 0) {
            throw new IllegalArgumentException("Kwota musi byc dodatnia");
        }
        GamePlayer from = session.getPlayers().stream()
                .filter(p -> p.getUser() != null && p.getUser().getUsername().equals(username))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Nie grasz w tej sesji"));
        GamePlayer to = session.getPlayers().stream()
                .filter(p -> p.getId().equals(toPlayerId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Brak odbiorcy"));
        if (from.getId().equals(to.getId())) {
            throw new IllegalArgumentException("Nie mozesz przelac siana samemu sobie");
        }
        if (from.getCash() < amount) {
            throw new IllegalArgumentException("Za malo siana (masz " + from.getCash() + " PLN)");
        }
        from.setCash(from.getCash() - amount);
        to.setCash(to.getCash() + amount);
        sessionRepository.save(session);

        String msg = from.getDisplayName() + " przelal " + amount + " PLN do " + to.getDisplayName() + ".";
        publishPublic(sessionId, null, null, msg, null, null, null);
        return toState(session, username, null, null, msg, null, null, null);
    }

    /** Stan bez personalizacji isMe — do broadcastu WebSocket. */
    public GameStateDto buildPublicState(Long sessionId, Integer d1, Integer d2, String message,
                                         Long movedId, Integer fromPos, Integer toPos) {
        GameSession session = getSession(sessionId);
        return toState(session, null, d1, d2, message, movedId, fromPos, toPos);
    }

    private void applyTileEffect(GamePlayer player, int pos, StringBuilder msg) {
        switch (pos) {
            case 4 -> {
                player.setCash(player.getCash() - 200);
                msg.append("Oplaca warunki (-200 PLN). ");
            }
            case POS_JAIL -> {
                player.setCash(Math.max(0, player.getCash() - 100));
                msg.append("Dziekanat (wiezienie): lapowka 100 PLN za wyjscie. ");
            }
            case 38 -> {
                player.setCash(player.getCash() - 100);
                msg.append("Oplata luksusowa (-100 PLN). ");
            }
            case 2, 17, 33 -> {
                player.setCash(player.getCash() + 200);
                msg.append("Kasa Miejska — stypendium (+200 PLN). ");
            }
            default -> { /* nieruchomosci — kupno w kolejnej iteracji */ }
        }
        if (player.getCash() < 0) {
            player.setBankrupt(true);
            msg.append("BANKRUCTWO! ");
        }
    }

    private void publishPublic(Long sessionId, Integer d1, Integer d2, String message,
                               Long movedId, Integer fromPos, Integer toPos) {
        gameSyncService.broadcast(sessionId,
                buildPublicState(sessionId, d1, d2, message, movedId, fromPos, toPos));
    }

    private void advanceTurn(GameSession session) {
        List<GamePlayer> players = session.getPlayers();
        int n = players.size();
        if (n == 0) return;
        int next = session.getCurrentTurn();
        for (int i = 0; i < n; i++) {
            next = (next + 1) % n;
            if (!players.get(next).isBankrupt()) break;
        }
        session.setCurrentTurn(next);
    }

    private void requireParticipant(GameSession session, String username) {
        boolean ok = session.getPlayers().stream()
                .anyMatch(p -> p.getUser() != null && p.getUser().getUsername().equals(username));
        if (!ok) {
            throw new IllegalArgumentException("Nie jestes uczestnikiem tej rozgrywki");
        }
    }

    private GameStateDto toState(GameSession session, String username,
                                 Integer d1, Integer d2, String message,
                                 Long movedId, Integer fromPos, Integer toPos) {
        List<GamePlayer> players = session.getPlayers();
        List<GamePlayerDto> dtos = new ArrayList<>();
        for (GamePlayer p : players) {
            boolean isMe = username != null && p.getUser() != null
                    && p.getUser().getUsername().equals(username);
            dtos.add(new GamePlayerDto(p.getId(), p.getDisplayName(), p.getCash(),
                    p.getPosition(), p.getColor(), p.isBankrupt(), isMe));
        }
        Long currentId = players.isEmpty() ? null
                : players.get(session.getCurrentTurn() % players.size()).getId();
        boolean myTurn = username != null && currentId != null && dtos.stream()
                .anyMatch(d -> d.isMe() && d.id().equals(currentId));

        return new GameStateDto(session.getId(), session.getCode(), session.getName(),
                session.getStatus().name(), dtos, currentId, d1, d2, message,
                movedId, fromPos, toPos, myTurn, tileNamesList(), tileEffectsList());
    }

    private String generateCode() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        String code;
        do {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 6; i++) {
                sb.append(chars.charAt(ThreadLocalRandom.current().nextInt(chars.length())));
            }
            code = sb.toString();
        } while (sessionRepository.existsByCode(code));
        return code;
    }
}
