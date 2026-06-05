package pl.pb.monopoly.service;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import pl.pb.monopoly.domain.*;
import pl.pb.monopoly.dto.*;
import pl.pb.monopoly.repository.GameSessionRepository;
import pl.pb.monopoly.repository.MatchHistoryRepository;
import pl.pb.monopoly.repository.OwnedItemRepository;
import pl.pb.monopoly.repository.UserRepository;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Silnik rozgrywki Kampus PB: ruch po planszy, Dziekanat (wiezienie),
 * podatki, transfery siana, KUPOWANIE pol, CZYNSZ, KARTY SZANSY oraz licytacje.
 * Stan synchronizowany przez WebSocket.
 */
@Service
public class GameService {

    /** 40 pol — Politechnika Bialostocka + Bialystok (nazwy). */
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

    /** Ceny zakupu pol. -1 = nie do kupienia. */
    public static final int[] TILE_PRICE = {
            -1, 140, -1, 140, -1, 200, 280, -1, 260, 260,
            -1, 180, 240, 160, 220, 200, 320, -1, 240, 220,
            -1, 300, -1, 260, 350, 200, 280, 260, 150, 240,
            -1, 200, 180, -1, 400, 200, -1, 380, -1, -1
    };

    /** Bazowy czynsz (gdy gracz posiada pojedyncze pole). 0 = nie do kupienia. */
    public static final int[] TILE_RENT = {
            0, 14, 0, 14, 0, 25, 24, 0, 22, 22,
            0, 18, 20, 16, 22, 25, 32, 0, 24, 22,
            0, 30, 0, 26, 35, 25, 28, 26, 15, 24,
            0, 20, 18, 0, 40, 25, 0, 38, 0, 0
    };

    public static final int POS_START = 0;
    public static final int POS_JAIL = 10;
    public static final int POS_GO_TO_JAIL = 30;

    /** Pola losowania kart. */
    private static final boolean[] CHANCE_TILES = new boolean[40];
    static {
        CHANCE_TILES[7] = true;
        CHANCE_TILES[22] = true;
        CHANCE_TILES[36] = true;
    }

    /** Pola dworcow (czynsz mnozony razy liczbe posiadanych dworcow). */
    private static final boolean[] STATION_TILES = new boolean[40];
    static {
        STATION_TILES[5] = true;
        STATION_TILES[15] = true;
        STATION_TILES[25] = true;
        STATION_TILES[35] = true;
    }

    /** Pole "Wodociagi PB" — czynsz x4 razy suma oczek. */
    private static final int POS_UTILITY = 28;

    /** Karty Szansy — humorystyczne, studenckie z PB i Bialegostoku. */
    public static final List<ChanceCard> CHANCE_CARDS = List.of(
            new ChanceCard("Stypendium rektora", "Otrzymujesz stypendium naukowe.", 150),
            new ChanceCard("Stypendium socjalne", "Komisja stypendialna sie zlitowala.", 100),
            new ChanceCard("Wpis warunkowy", "Wykladowca daje warunek za 50 PLN.", -50),
            new ChanceCard("Mandat za rower na deptaku", "Straz miejska nie spi.", -75),
            new ChanceCard("Wygrana w Klubie Gwint", "Wieczor pokerowy w Gwincie konczy sie w bloku.", 200),
            new ChanceCard("Awaria kuchenki w akademiku", "Skladasz sie z Kolegami na nowa.", -80),
            new ChanceCard("Wsparcie z parafii", "Babcia przyslala 60 zl w kopercie.", 60),
            new ChanceCard("Zaliczenie z marszu", "Wykladowca dostal kawe — daje plusa.", 0),
            new ChanceCard("Zwrot z USOSweb", "Pomylka w czesnym, dziekanat zwraca.", 120),
            new ChanceCard("Awaria laptopa przed sesja", "Naprawa kosztuje krocie.", -200),
            new ChanceCard("Spalony kolokwiom", "Korepetycje przed poprawka.", -120),
            new ChanceCard("Wygrana w Konkursie Naukowym", "Twoja praca zwyciezyla na konferencji PB.", 300),
            new ChanceCard("Bilet ulgowy z PKP", "Oszczednosc na wyjezdzie do domu.", 30),
            new ChanceCard("Zwiedzanie Palacu Branickich", "Bilet studencki gratis, ale zalezles z Patelnia.", -25),
            new ChanceCard("Zlapal Cie strażnik biblioteczny", "Zwrot ksiazek po terminie.", -40),
            new ChanceCard("Praca dorywcza w stolowce", "Tydzien zmywania naczyn.", 100),
            new ChanceCard("Zgubiona legitymacja", "Wyrobienie nowej kosztuje.", -45),
            new ChanceCard("Voucher na obiad w stolowce", "Stolowka studencka funduje obiad.", 25),
            new ChanceCard("Babcia odwiedzila", "Zostawila siano i sloiki z ogorkami.", 80),
            new ChanceCard("Korki ze studentem 5. roku", "Tlumacza Ci algorytmy.", -55)
    );

    private static final String[] COLORS = {
            "#38bdf8", "#a855f7", "#10b981", "#f59e0b", "#f43f5e", "#6366f1"
    };

    private final GameSessionRepository sessionRepository;
    private final UserRepository userRepository;
    private final GameSyncService gameSyncService;
    private final MatchHistoryRepository matchHistoryRepository;
    private final OwnedItemRepository ownedItemRepository;
    private BotAutoplayService botAutoplayService;

    public GameService(GameSessionRepository sessionRepository,
                       UserRepository userRepository,
                       @Lazy GameSyncService gameSyncService,
                       MatchHistoryRepository matchHistoryRepository,
                       OwnedItemRepository ownedItemRepository) {
        this.sessionRepository = sessionRepository;
        this.userRepository = userRepository;
        this.gameSyncService = gameSyncService;
        this.matchHistoryRepository = matchHistoryRepository;
        this.ownedItemRepository = ownedItemRepository;
    }

    /** Setter dla autoplay - lazy by uniknac cyklicznej zaleznosci konstruktorow. */
    @org.springframework.beans.factory.annotation.Autowired
    public void setBotAutoplayService(@Lazy BotAutoplayService botAutoplayService) {
        this.botAutoplayService = botAutoplayService;
    }

    /**
     * Wywoluje onTurnUpdate PO commicie biezacej transakcji.
     * Dzieki temu bot widzi zaaktualizowany stan w BD i nie wpada w petle
     * wynikajaca z odczytu niecommitowanego stanu.
     */
    private void scheduleBotUpdate(Long sessionId) {
        if (botAutoplayService == null) return;
        final BotAutoplayService bot = botAutoplayService;
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    bot.onTurnUpdate(sessionId);
                }
            });
        } else {
            bot.onTurnUpdate(sessionId);
        }
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
        GamePlayer me = new GamePlayer(creator.getUsername(), equippedColor(creator, COLORS[0]));
        me.setUser(creator);
        session.addPlayer(me);

        if (friendIds != null) {
            for (Long fid : friendIds) {
                userRepository.findById(fid).ifPresent(friend -> {
                    if (session.getPlayers().stream().noneMatch(p ->
                            p.getUser() != null && p.getUser().getId().equals(friend.getId()))) {
                        String color = equippedColor(friend,
                                COLORS[session.getPlayers().size() % COLORS.length]);
                        GamePlayer gp = new GamePlayer(friend.getUsername(), color);
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
        publishPublic(saved.getId(), null, null, null, null, null, null, null);
        scheduleBotUpdate(saved.getId());
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
            publishPublic(session.getId(), null, null, u.getUsername() + " dolaczyl do gry!", null, null, null, null);
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
        return toState(getSession(sessionId), username, null, null, null, null, null, null, null);
    }

    /** Rzut kostka — tylko gracz z aktualna tura, bez aktywnej decyzji o kupnie. */
    @Transactional
    public GameStateDto roll(Long sessionId, String username) {
        GameSession session = getSession(sessionId);
        requireParticipant(session, username);
        requireActiveGame(session);
        if (session.getPendingPurchasePos() != null) {
            throw new IllegalArgumentException("Najpierw zakoncz decyzje o polu (kup lub pomin).");
        }
        if (session.getPendingPaymentDebtorId() != null) {
            throw new IllegalArgumentException("Trwa uregulowanie zaleglosci — najpierw oplac lub zbankrutuj.");
        }
        List<GamePlayer> players = session.getPlayers();
        if (players.isEmpty()) {
            return toState(session, username, null, null, "Brak graczy.", null, null, null, null);
        }

        GamePlayer current = players.get(session.getCurrentTurn() % players.size());
        if (current.getUser() == null || !current.getUser().getUsername().equals(username)) {
            throw new IllegalArgumentException("To nie Twoja tura! Czekaj na ruch innego gracza.");
        }
        if (current.isBankrupt()) {
            throw new IllegalArgumentException("Jestes bankrutem i nie mozesz grac.");
        }
        if (current.getCash() < 0) {
            throw new IllegalArgumentException("Masz ujemne saldo — ureguluj zaleglosci.");
        }

        return doRoll(session, current, username);
    }

    /** Rzut kostka wykonany przez bota (uzywany przez BotAutoplayService). */
    @Transactional
    public GameStateDto rollAsBot(Long sessionId, Long botPlayerId) {
        GameSession session = getSession(sessionId);
        if (session.getStatus() == GameStatus.FINISHED) return null;
        if (session.getPendingPurchasePos() != null) return null;
        if (session.getPendingPaymentDebtorId() != null) return null;
        List<GamePlayer> players = session.getPlayers();
        if (players.isEmpty()) return null;
        GamePlayer current = players.get(session.getCurrentTurn() % players.size());
        if (!current.getId().equals(botPlayerId)) return null;
        if (current.getUser() != null) return null;
        if (current.isBankrupt()) return null;
        if (current.getCash() < 0) return null;
        return doRoll(session, current, null);
    }

    /** Rdzen logiki rzutu - wspoldzielony przez {@link #roll} i {@link #rollAsBot}. */
    private GameStateDto doRoll(GameSession session, GamePlayer current, String username) {
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
            msg.append("Trafia na pole \"Idz do Dziekanatu\" - laduje w WIEZIENIU! ");
        }

        current.setPosition(newPos);
        msg.append("Staje na: ").append(TILES[newPos]).append(". ");

        ChanceCard drawnCard = null;
        if (CHANCE_TILES[newPos]) {
            drawnCard = drawCard();
            msg.append("Karta Szansy: \"").append(drawnCard.title())
                    .append("\" - ").append(drawnCard.description()).append(" ");
            int effect = drawnCard.moneyEffect();
            if (effect > 0) {
                current.setCash(current.getCash() + effect);
                msg.append("(+").append(effect).append(" PLN). ");
            } else if (effect < 0) {
                chargePlayer(session, current, -effect, null, "Karta Szansy: " + drawnCard.title(), msg);
                msg.append("(-").append(-effect).append(" PLN). ");
            }
        } else {
            applyTileEffect(current, newPos, steps, session, msg);
        }

        if (drawnCard == null && TILE_PRICE[newPos] > 0) {
            GamePlayer ownerOfTile = findOwner(session, newPos);
            if (ownerOfTile == null) {
                if (current.getCash() >= TILE_PRICE[newPos]) {
                    session.setPendingPurchasePos(newPos);
                    session.setPendingPurchasePrice(TILE_PRICE[newPos]);
                    session.setPendingDeciderId(current.getId());
                    msg.append("Mozesz kupic to pole za ").append(TILE_PRICE[newPos])
                            .append(" PLN, lub pominac (licytacja). ");
                } else {
                    msg.append("Za malo siana, by kupic - pole zostaje wolne. ");
                }
            } else if (!ownerOfTile.getId().equals(current.getId()) && !ownerOfTile.isBankrupt()) {
                int rent = computeRent(session, ownerOfTile, newPos, steps);
                if (chargePlayer(session, current, rent, ownerOfTile,
                        "Czynsz: " + TILES[newPos], msg)) {
                    msg.append(current.getDisplayName()).append(" placi czynsz ")
                            .append(rent).append(" PLN dla ")
                            .append(ownerOfTile.getDisplayName()).append(". ");
                }
            } else if (ownerOfTile.getId().equals(current.getId())) {
                msg.append("To Twoje pole - bez czynszu. ");
            }
        }

        if (session.getPendingPurchasePos() == null && session.getPendingPaymentDebtorId() == null) {
            advanceTurn(session);
        }
        checkGameEnd(session, msg);
        sessionRepository.save(session);

        Long movedId = current.getId();
        ChanceCardDto cardDto = drawnCard != null
                ? new ChanceCardDto(drawnCard.title(), drawnCard.description(), drawnCard.moneyEffect())
                : null;
        Long sessionId = session.getId();
        publishPublic(sessionId, d1, d2, msg.toString(), movedId, oldPos, newPos, cardDto);

        // Powiadom autoplay (boty rzucaja same, decyzja czeka 10s)
        scheduleBotUpdate(sessionId);
        return toState(session, username, d1, d2, msg.toString(), movedId, oldPos, newPos, cardDto);
    }

    /** Aktywny gracz kupuje pole, na ktorym sie znajduje. */
    @Transactional
    public GameStateDto buy(Long sessionId, String username) {
        GameSession session = getSession(sessionId);
        requireParticipant(session, username);
        if (session.getPendingPurchasePos() == null) {
            throw new IllegalArgumentException("Brak aktywnego pola do kupna.");
        }
        GamePlayer me = findPlayerByUsername(session, username);
        if (!me.getId().equals(session.getPendingDeciderId())) {
            throw new IllegalArgumentException("To nie Ty stoisz na tym polu.");
        }
        return doBuy(session, me, username);
    }

    /** Bot kupuje pole - bez sprawdzania username. */
    @Transactional
    public GameStateDto buyAsBot(Long sessionId, Long botPlayerId) {
        GameSession session = getSession(sessionId);
        if (session.getPendingPurchasePos() == null) return null;
        if (!botPlayerId.equals(session.getPendingDeciderId())) return null;
        GamePlayer me = session.getPlayers().stream()
                .filter(p -> p.getId().equals(botPlayerId))
                .findFirst().orElse(null);
        if (me == null || me.getUser() != null) return null;
        return doBuy(session, me, null);
    }

    private GameStateDto doBuy(GameSession session, GamePlayer me, String username) {
        int pos = session.getPendingPurchasePos();
        int price = session.getPendingPurchasePrice();
        if (me.getCash() < price) {
            throw new IllegalArgumentException("Za malo siana na zakup.");
        }
        me.setCash(me.getCash() - price);
        me.getOwnedPositions().add(pos);
        session.clearPendingPurchase();
        advanceTurn(session);
        sessionRepository.save(session);

        String msg = me.getDisplayName() + " kupuje " + TILES[pos] + " za " + price + " PLN.";
        Long sessionId = session.getId();
        publishPublic(sessionId, null, null, msg, null, null, null, null);
        scheduleBotUpdate(sessionId);
        return toState(session, username, null, null, msg, null, null, null, null);
    }

    /** Aktywny gracz pomija kupno - inni mogli wczesniej zalicytowac, ale w tej wersji pole zostaje wolne. */
    @Transactional
    public GameStateDto skipPurchase(Long sessionId, String username) {
        GameSession session = getSession(sessionId);
        requireParticipant(session, username);
        if (session.getPendingPurchasePos() == null) {
            throw new IllegalArgumentException("Brak aktywnego pola.");
        }
        GamePlayer me = findPlayerByUsername(session, username);
        if (!me.getId().equals(session.getPendingDeciderId())) {
            throw new IllegalArgumentException("To nie Ty mozesz pominac.");
        }
        return doSkip(session, me, username, false);
    }

    /** Bot pomija pole. */
    @Transactional
    public GameStateDto skipAsBot(Long sessionId, Long botPlayerId) {
        GameSession session = getSession(sessionId);
        if (session.getPendingPurchasePos() == null) return null;
        if (!botPlayerId.equals(session.getPendingDeciderId())) return null;
        GamePlayer me = session.getPlayers().stream()
                .filter(p -> p.getId().equals(botPlayerId))
                .findFirst().orElse(null);
        if (me == null) return null;
        return doSkip(session, me, null, false);
    }

    /**
     * Decyzja bota: kupuje gdy ma >= 2x ceny w gotowce (zostanie poduszka),
     * w przeciwnym razie pomija. Wszystko w jednej transakcji.
     */
    @Transactional
    public GameStateDto botDecide(Long sessionId, Long botPlayerId, int snapPos) {
        GameSession session = getSession(sessionId);
        if (session.getPendingPurchasePos() == null) return null;
        if (session.getPendingPurchasePos() != snapPos) return null;
        if (!botPlayerId.equals(session.getPendingDeciderId())) return null;
        GamePlayer bot = session.getPlayers().stream()
                .filter(p -> p.getId().equals(botPlayerId))
                .findFirst().orElse(null);
        if (bot == null || bot.getUser() != null) return null;
        int price = session.getPendingPurchasePrice() != null ? session.getPendingPurchasePrice() : 0;
        if (bot.getCash() >= price * 2) {
            return doBuy(session, bot, null);
        }
        return doSkip(session, bot, null, false);
    }

    /** Auto-skip wywolany przez timeout (10s). Wymusza pominiecie dla obecnego decydera. */
    @Transactional
    public GameStateDto autoSkipTimeout(Long sessionId, int snapPosition) {
        GameSession session = getSession(sessionId);
        if (session.getPendingPurchasePos() == null) return null;
        if (session.getPendingPurchasePos() != snapPosition) return null;
        Long deciderId = session.getPendingDeciderId();
        if (deciderId == null) return null;
        GamePlayer me = session.getPlayers().stream()
                .filter(p -> p.getId().equals(deciderId))
                .findFirst().orElse(null);
        if (me == null) return null;
        return doSkip(session, me, null, true);
    }

    private GameStateDto doSkip(GameSession session, GamePlayer me, String username, boolean timeout) {
        int pos = session.getPendingPurchasePos();
        session.clearPendingPurchase();
        advanceTurn(session);
        sessionRepository.save(session);

        String msg = timeout
                ? me.getDisplayName() + " nie zdecydowal sie w czasie - pole " + TILES[pos] + " zostaje wolne."
                : me.getDisplayName() + " pomija pole " + TILES[pos] + " - zostaje wolne.";
        Long sessionId = session.getId();
        publishPublic(sessionId, null, null, msg, null, null, null, null);
        scheduleBotUpdate(sessionId);
        return toState(session, username, null, null, msg, null, null, null, null);
    }

    /**
     * Inny gracz (nie stojacy na polu) sklada oferte licytacji rowna lub wieksza
     * od ceny minimalnej. Kupno natychmiastowe — najszybsza oferta wygrywa.
     */
    @Transactional
    public GameStateDto bid(Long sessionId, String username, int amount) {
        GameSession session = getSession(sessionId);
        requireParticipant(session, username);
        if (session.getPendingPurchasePos() == null) {
            throw new IllegalArgumentException("Brak aktywnej licytacji.");
        }
        GamePlayer me = findPlayerByUsername(session, username);
        if (me.getId().equals(session.getPendingDeciderId())) {
            throw new IllegalArgumentException("Aktywny gracz nie licytuje - kup zwyklym przyciskiem.");
        }
        int basePrice = session.getPendingPurchasePrice();
        if (amount < Math.max(50, basePrice / 2)) {
            throw new IllegalArgumentException("Minimalna oferta to " + Math.max(50, basePrice / 2) + " PLN.");
        }
        if (me.getCash() < amount) {
            throw new IllegalArgumentException("Za malo siana na licytacje.");
        }
        int pos = session.getPendingPurchasePos();
        me.setCash(me.getCash() - amount);
        me.getOwnedPositions().add(pos);
        session.clearPendingPurchase();
        advanceTurn(session);
        sessionRepository.save(session);

        String msg = me.getDisplayName() + " wygrywa licytacje na " + TILES[pos] + " za " + amount + " PLN!";
        publishPublic(sessionId, null, null, msg, null, null, null, null);
        scheduleBotUpdate(sessionId);
        return toState(session, username, null, null, msg, null, null, null, null);
    }

    @Transactional
    public GameStateDto transfer(Long sessionId, String username, Long toPlayerId, int amount) {
        GameSession session = getSession(sessionId);
        requireActiveGame(session);
        if (amount <= 0) {
            throw new IllegalArgumentException("Kwota musi byc dodatnia");
        }
        GamePlayer from = findPlayerByUsername(session, username);
        GamePlayer to = session.getPlayers().stream()
                .filter(p -> p.getId().equals(toPlayerId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Brak odbiorcy"));
        if (from.getId().equals(to.getId())) {
            throw new IllegalArgumentException("Nie mozesz przelac siana samemu sobie");
        }
        if (from.isBankrupt()) {
            throw new IllegalArgumentException("Bankrut nie moze przelac siana.");
        }
        if (to.isBankrupt()) {
            throw new IllegalArgumentException("Nie mozna przelac siana bankrutowi.");
        }
        if (from.getCash() < amount) {
            throw new IllegalArgumentException("Za malo siana (masz " + from.getCash() + " PLN)");
        }
        from.setCash(from.getCash() - amount);
        to.setCash(to.getCash() + amount);
        sessionRepository.save(session);

        String msg = from.getDisplayName() + " przelal " + amount + " PLN do " + to.getDisplayName() + ".";
        if (session.getPendingPaymentDebtorId() != null
                && session.getPendingPaymentDebtorId().equals(to.getId())) {
            msg += " (pozyczka na splate zadluzenia)";
        }
        publishPublic(sessionId, null, null, msg, null, null, null, null);
        return toState(session, username, null, null, msg, null, null, null, null);
    }

    /** Sprzedaz nieruchomosci (50% ceny) w fazie uregulowania zadluzenia. */
    @Transactional
    public GameStateDto sellProperty(Long sessionId, String username, int position) {
        GameSession session = getSession(sessionId);
        requireActiveGame(session);
        if (session.getPendingPaymentDebtorId() == null) {
            throw new IllegalArgumentException("Brak aktywnego zadluzenia do splaty.");
        }
        GamePlayer me = findPlayerByUsername(session, username);
        if (!me.getId().equals(session.getPendingPaymentDebtorId())) {
            throw new IllegalArgumentException("Tylko zadluzony gracz moze sprzedawac nieruchomosci.");
        }
        String msg = doSellProperty(me, position);
        sessionRepository.save(session);
        Long sessionIdVal = session.getId();
        publishPublic(sessionIdVal, null, null, msg, null, null, null, null);
        return toState(session, username, null, null, msg, null, null, null, null);
    }

    /** Zadluzony gracz oplaca zaleglosc po zebraniu wystarczajacej kwoty. */
    @Transactional
    public GameStateDto payDebt(Long sessionId, String username) {
        GameSession session = getSession(sessionId);
        requireActiveGame(session);
        if (session.getPendingPaymentDebtorId() == null) {
            throw new IllegalArgumentException("Brak zadluzenia do splaty.");
        }
        GamePlayer me = findPlayerByUsername(session, username);
        if (!me.getId().equals(session.getPendingPaymentDebtorId())) {
            throw new IllegalArgumentException("To nie Twoje zadluzenie.");
        }
        return doPayDebt(session, me, username);
    }

    /** Zadluzony gracz rezygnuje — bankructwo. */
    @Transactional
    public GameStateDto declareBankruptcy(Long sessionId, String username) {
        GameSession session = getSession(sessionId);
        requireActiveGame(session);
        if (session.getPendingPaymentDebtorId() == null) {
            throw new IllegalArgumentException("Brak aktywnego zadluzenia.");
        }
        GamePlayer me = findPlayerByUsername(session, username);
        if (!me.getId().equals(session.getPendingPaymentDebtorId())) {
            throw new IllegalArgumentException("To nie Twoje zadluzenie.");
        }
        return doDeclareBankruptcy(session, me, username);
    }

    /** Bot/automat: sprzedaje pola i splaca lub bankrutuje. */
    @Transactional
    public GameStateDto resolvePaymentAsBot(Long sessionId, Long debtorId, int snapAmount) {
        GameSession session = getSession(sessionId);
        if (session.getPendingPaymentDebtorId() == null) return null;
        if (!debtorId.equals(session.getPendingPaymentDebtorId())) return null;
        if (session.getPendingPaymentAmount() == null || session.getPendingPaymentAmount() != snapAmount) return null;
        GamePlayer debtor = session.getPlayers().stream()
                .filter(p -> p.getId().equals(debtorId))
                .findFirst().orElse(null);
        if (debtor == null || debtor.isBankrupt()) return null;

        StringBuilder msg = new StringBuilder();
        while (debtor.getCash() < snapAmount && !debtor.getOwnedPositions().isEmpty()) {
            int cheapest = cheapestOwnedPosition(debtor);
            msg.append(doSellProperty(debtor, cheapest)).append(" ");
        }
        if (debtor.getCash() >= snapAmount) {
            if (!msg.isEmpty()) publishPublic(sessionId, null, null, msg.toString(), null, null, null, null);
            return doPayDebt(session, debtor, null);
        }
        if (!msg.isEmpty()) publishPublic(sessionId, null, null, msg.toString(), null, null, null, null);
        return doDeclareBankruptcy(session, debtor, null);
    }

    /** Timeout dla czlowieka w fazie splaty — auto jak bot. */
    @Transactional
    public GameStateDto autoPaymentTimeout(Long sessionId, int snapAmount) {
        GameSession session = getSession(sessionId);
        if (session.getPendingPaymentDebtorId() == null) return null;
        if (session.getPendingPaymentAmount() == null || session.getPendingPaymentAmount() != snapAmount) return null;
        Long debtorId = session.getPendingPaymentDebtorId();
        return resolvePaymentAsBot(sessionId, debtorId, snapAmount);
    }

    public GameStateDto buildPublicState(Long sessionId, Integer d1, Integer d2, String message,
                                         Long movedId, Integer fromPos, Integer toPos,
                                         ChanceCardDto card) {
        GameSession session = getSession(sessionId);
        return toState(session, null, d1, d2, message, movedId, fromPos, toPos, card);
    }

    private void applyTileEffect(GamePlayer player, int pos, int diceSum, GameSession session, StringBuilder msg) {
        switch (pos) {
            case 4 -> {
                chargePlayer(session, player, 200, null, "Oplata za warunki", msg);
                msg.append("Oplata za warunki (-200 PLN). ");
            }
            case POS_JAIL -> {
                if (chargePlayer(session, player, 100, null, "Lapowka w Dziekanacie", msg)) {
                    msg.append("Dziekanat (wiezienie): lapowka 100 PLN. ");
                } else {
                    msg.append("Dziekanat (wiezienie): lapowka 100 PLN — brak siana! ");
                }
            }
            case 38 -> {
                chargePlayer(session, player, 100, null, "Oplata luksusowa", msg);
                msg.append("Oplata luksusowa (-100 PLN). ");
            }
            case 2, 17, 33 -> {
                player.setCash(player.getCash() + 200);
                msg.append("Kasa Miejska - stypendium (+200 PLN). ");
            }
            default -> { /* posiadlosci - obsluga w roll() */ }
        }
    }

    private ChanceCard drawCard() {
        return CHANCE_CARDS.get(ThreadLocalRandom.current().nextInt(CHANCE_CARDS.size()));
    }

    /** Znajduje wlasciciela danego pola w sesji (lub null). */
    private GamePlayer findOwner(GameSession session, int pos) {
        for (GamePlayer p : session.getPlayers()) {
            if (p.getOwnedPositions().contains(pos)) return p;
        }
        return null;
    }

    private GamePlayer findPlayerByUsername(GameSession session, String username) {
        return session.getPlayers().stream()
                .filter(p -> p.getUser() != null && p.getUser().getUsername().equals(username))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Nie grasz w tej sesji"));
    }

    /** Liczy czynsz na podstawie typu pola (zwykle / dworzec / wodociagi) i posiadanych pol. */
    private int computeRent(GameSession session, GamePlayer owner, int pos, int diceSum) {
        if (STATION_TILES[pos]) {
            int stations = 0;
            for (int i = 0; i < 40; i++) if (STATION_TILES[i] && owner.getOwnedPositions().contains(i)) stations++;
            return 25 * stations;
        }
        if (pos == POS_UTILITY) {
            return diceSum * 4;
        }
        return TILE_RENT[pos];
    }

    /** Po bankructwie zwalnia pola gracza. */
    private void releaseProperties(GamePlayer player) {
        player.getOwnedPositions().clear();
    }

    private void requireActiveGame(GameSession session) {
        if (session.getStatus() == GameStatus.FINISHED) {
            throw new IllegalArgumentException("Gra zostala zakonczona.");
        }
    }

    /** Cena sprzedazy nieruchomosci — polowa ceny zakupu. */
    public static int sellPrice(int position) {
        int price = TILE_PRICE[position];
        return price > 0 ? price / 2 : 0;
    }

    /**
     * Pobiera oplate od gracza. Gdy brak siana — uruchamia faze splaty zamiast ujemnego salda.
     * @return true gdy oplacono od razu, false gdy trwa pending payment
     */
    private boolean chargePlayer(GameSession session, GamePlayer debtor, int amount,
                                 GamePlayer creditor, String reason, StringBuilder msg) {
        if (amount <= 0) return true;
        if (debtor.getCash() >= amount) {
            debtor.setCash(debtor.getCash() - amount);
            if (creditor != null) {
                creditor.setCash(creditor.getCash() + amount);
            }
            return true;
        }
        session.setPendingPaymentDebtorId(debtor.getId());
        session.setPendingPaymentAmount(amount);
        session.setPendingPaymentCreditorId(creditor != null ? creditor.getId() : null);
        session.setPendingPaymentReason(reason);
        msg.append("Brakuje siana! ").append(debtor.getDisplayName())
                .append(" musi uzbierac ").append(amount).append(" PLN (ma ")
                .append(debtor.getCash()).append("). Sprzedaj nieruchomosc lub popros o pozyczke. ");
        return false;
    }

    private String doSellProperty(GamePlayer player, int position) {
        if (!player.getOwnedPositions().contains(position)) {
            throw new IllegalArgumentException("Nie posiadasz tego pola.");
        }
        int salePrice = sellPrice(position);
        if (salePrice <= 0) {
            throw new IllegalArgumentException("To pole nie jest na sprzedaz.");
        }
        player.getOwnedPositions().remove(position);
        player.setCash(player.getCash() + salePrice);
        return player.getDisplayName() + " sprzedaje " + TILES[position] + " za " + salePrice + " PLN.";
    }

    private GameStateDto doPayDebt(GameSession session, GamePlayer debtor, String username) {
        int amount = session.getPendingPaymentAmount();
        if (debtor.getCash() < amount) {
            throw new IllegalArgumentException("Nadal za malo siana (potrzeba " + amount + " PLN, masz "
                    + debtor.getCash() + "). Sprzedaj nieruchomosc lub popros o pozyczke.");
        }
        GamePlayer creditor = null;
        if (session.getPendingPaymentCreditorId() != null) {
            creditor = session.getPlayers().stream()
                    .filter(p -> p.getId().equals(session.getPendingPaymentCreditorId()))
                    .findFirst().orElse(null);
        }
        debtor.setCash(debtor.getCash() - amount);
        if (creditor != null && !creditor.isBankrupt()) {
            creditor.setCash(creditor.getCash() + amount);
        }
        String reason = session.getPendingPaymentReason() != null ? session.getPendingPaymentReason() : "Oplata";
        session.clearPendingPayment();
        StringBuilder msg = new StringBuilder(debtor.getDisplayName() + " oplaca " + amount + " PLN (" + reason + "). ");
        advanceTurn(session);
        checkGameEnd(session, msg);
        sessionRepository.save(session);
        Long sessionId = session.getId();
        publishPublic(sessionId, null, null, msg.toString(), null, null, null, null);
        scheduleBotUpdate(sessionId);
        return toState(session, username, null, null, msg.toString(), null, null, null, null);
    }

    private GameStateDto doDeclareBankruptcy(GameSession session, GamePlayer debtor, String username) {
        StringBuilder msg = new StringBuilder();
        applyBankruptcy(debtor, msg);
        session.clearPendingPayment();
        advanceTurn(session);
        checkGameEnd(session, msg);
        sessionRepository.save(session);
        Long sessionId = session.getId();
        publishPublic(sessionId, null, null, msg.toString(), null, null, null, null);
        scheduleBotUpdate(sessionId);
        return toState(session, username, null, null, msg.toString(), null, null, null, null);
    }

    private void applyBankruptcy(GamePlayer player, StringBuilder msg) {
        player.setBankrupt(true);
        player.setCash(0);
        releaseProperties(player);
        msg.append(player.getDisplayName()).append(" BANKRUCTWO! ");
    }

    private void checkGameEnd(GameSession session, StringBuilder msg) {
        if (session.getStatus() == GameStatus.FINISHED) return;
        List<GamePlayer> active = session.getPlayers().stream()
                .filter(p -> !p.isBankrupt())
                .toList();
        if (active.size() == 1) {
            session.setStatus(GameStatus.FINISHED);
            GamePlayer winner = active.get(0);
            msg.append(winner.getDisplayName()).append(" WYGRYWA GRE! ");
            persistGameResults(session, winner);
        } else if (active.isEmpty()) {
            session.setStatus(GameStatus.FINISHED);
            msg.append("Wszyscy bankruci — remis. ");
            persistGameResults(session, null);
        }
    }

    /** Zapisuje MatchHistory i aktualizuje PlayerStatistics po koncu gry. */
    private void persistGameResults(GameSession session, GamePlayer winner) {
        int totalPlayers = (int) session.getPlayers().stream().filter(p -> p.getUser() != null).count();
        if (totalPlayers == 0) return;
        long durationMinutes = ChronoUnit.MINUTES.between(session.getCreatedAt(), LocalDateTime.now());

        for (GamePlayer gp : session.getPlayers()) {
            if (gp.getUser() == null) continue;
            User user = userRepository.findById(gp.getUser().getId()).orElse(null);
            if (user == null) continue;
            boolean won = winner != null && winner.getId().equals(gp.getId());
            int eloChange = won ? 20 + (totalPlayers - 1) * 5 : -10;
            int placement = won ? 1 : 2;

            MatchHistory mh = new MatchHistory();
            mh.setUser(user);
            mh.setWon(won);
            mh.setPlacement(placement);
            mh.setPlayersCount(totalPlayers);
            mh.setFinalCash(gp.getCash());
            mh.setDurationMinutes((int) Math.max(1, durationMinutes));
            mh.setEloChange(eloChange);
            matchHistoryRepository.save(mh);

            PlayerStatistics stats = user.getStatistics();
            if (stats != null) {
                stats.setGamesPlayed(stats.getGamesPlayed() + 1);
                if (won) {
                    stats.setGamesWon(stats.getGamesWon() + 1);
                    stats.setWinStreak(stats.getWinStreak() + 1);
                } else {
                    stats.setWinStreak(0);
                }
                int newElo = Math.max(0, stats.getEloPoints() + eloChange);
                stats.setEloPoints(newElo);
                stats.setLevel(Math.max(1, 1 + newElo / 500));
            }
        }
    }

    /** Mapa slug koloru -> hex z katalogu lootbox. */
    private static final Map<String, String> PAWN_COLORS = Map.of(
            "color-blue",    "#38bdf8",
            "color-red",     "#f43f5e",
            "color-emerald", "#10b981",
            "color-pink",    "#ec4899",
            "color-neon",    "#a3e635"
    );

    /** Zwraca kolor pionka wg. zalozonego itemu gracza lub domyslny. */
    private String equippedColor(User user, String defaultColor) {
        if (user == null || user.getId() == null) return defaultColor;
        List<OwnedItem> equipped = ownedItemRepository.findByUserIdAndEquipped(user.getId(), true);
        for (OwnedItem item : equipped) {
            String hex = PAWN_COLORS.get(item.getItemSlug());
            if (hex != null) return hex;
        }
        return defaultColor;
    }

    private int cheapestOwnedPosition(GamePlayer player) {
        return player.getOwnedPositions().stream()
                .min((a, b) -> Integer.compare(sellPrice(a), sellPrice(b)))
                .orElseThrow();
    }

    private Map<Integer, Integer> sellPricesFor(GamePlayer player) {
        Map<Integer, Integer> prices = new HashMap<>();
        for (int pos : player.getOwnedPositions()) {
            int sp = sellPrice(pos);
            if (sp > 0) prices.put(pos, sp);
        }
        return prices;
    }

    private void publishPublic(Long sessionId, Integer d1, Integer d2, String message,
                               Long movedId, Integer fromPos, Integer toPos,
                               ChanceCardDto card) {
        gameSyncService.broadcast(sessionId,
                buildPublicState(sessionId, d1, d2, message, movedId, fromPos, toPos, card));
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
                                 Long movedId, Integer fromPos, Integer toPos,
                                 ChanceCardDto card) {
        List<GamePlayer> players = session.getPlayers();
        Map<Integer, Long> ownership = new HashMap<>();
        for (GamePlayer p : players) {
            for (Integer pos : p.getOwnedPositions()) ownership.put(pos, p.getId());
        }

        List<GamePlayerDto> dtos = new ArrayList<>();
        for (GamePlayer p : players) {
            boolean isMe = username != null && p.getUser() != null
                    && p.getUser().getUsername().equals(username);
            boolean isBot = p.getUser() == null;
            dtos.add(new GamePlayerDto(p.getId(), p.getDisplayName(), p.getCash(),
                    p.getPosition(), p.getColor(), p.isBankrupt(), isMe, isBot,
                    new ArrayList<>(p.getOwnedPositions())));
        }
        Long currentId = players.isEmpty() ? null
                : players.get(session.getCurrentTurn() % players.size()).getId();
        boolean myTurn = username != null && currentId != null && dtos.stream()
                .anyMatch(d -> d.isMe() && d.id().equals(currentId));

        PendingPurchaseDto pending = null;
        if (session.getPendingPurchasePos() != null) {
            pending = new PendingPurchaseDto(
                    session.getPendingPurchasePos(),
                    TILES[session.getPendingPurchasePos()],
                    session.getPendingPurchasePrice(),
                    session.getPendingDeciderId(),
                    Math.max(50, session.getPendingPurchasePrice() / 2)
            );
        }

        PendingPaymentDto pendingPayment = null;
        if (session.getPendingPaymentDebtorId() != null && session.getPendingPaymentAmount() != null) {
            GamePlayer debtor = players.stream()
                    .filter(p -> p.getId().equals(session.getPendingPaymentDebtorId()))
                    .findFirst().orElse(null);
            pendingPayment = new PendingPaymentDto(
                    session.getPendingPaymentDebtorId(),
                    session.getPendingPaymentAmount(),
                    session.getPendingPaymentCreditorId(),
                    session.getPendingPaymentReason(),
                    debtor != null ? sellPricesFor(debtor) : Map.of()
            );
        }

        Long winnerId = null;
        String winnerName = null;
        if (session.getStatus() == GameStatus.FINISHED) {
            List<GamePlayer> active = players.stream().filter(p -> !p.isBankrupt()).toList();
            if (active.size() == 1) {
                winnerId = active.get(0).getId();
                winnerName = active.get(0).getDisplayName();
            }
        }

        // Lista cen i grup pol — statyczne dane do rysowania UI
        List<Integer> prices = new ArrayList<>();
        for (int v : TILE_PRICE) prices.add(v);

        return new GameStateDto(session.getId(), session.getCode(), session.getName(),
                session.getStatus().name(), dtos, currentId, d1, d2, message,
                movedId, fromPos, toPos, myTurn, tileNamesList(), tileEffectsList(),
                pending, pendingPayment, ownership, prices, card, winnerId, winnerName);
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

    /** Wewnetrzny rekord karty Szansy. */
    public record ChanceCard(String title, String description, int moneyEffect) {}
}
