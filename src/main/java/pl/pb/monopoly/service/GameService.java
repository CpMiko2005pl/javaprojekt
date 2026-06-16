package pl.pb.monopoly.service;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import pl.pb.monopoly.domain.*;
import pl.pb.monopoly.dto.*;

import java.util.Arrays;
import java.util.Collections;
import pl.pb.monopoly.repository.GameInviteRepository;
import pl.pb.monopoly.repository.GameSessionRepository;
import pl.pb.monopoly.repository.MatchHistoryRepository;
import pl.pb.monopoly.repository.OwnedItemRepository;
import pl.pb.monopoly.repository.UserRepository;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
public class GameService {

    public static final String[] TILES = {
            "START — Inauguracja semestru",
            "Akademik Alfa",
            "Stypendium — Kasa Studencka",
            "Akademik Beta",
            "Nieoddany projekt — Podatek",
            "Dwor Mejera — Kurort",
            "Akademik Gamma",
            "USOS — Karta Szansy",
            "Akademik Delta",
            "Akademik Epsilon",
            "WARUNEK (Dziekanat)",
            "Klub GWINT",
            "Max Bistro",
            "Gammajka",
            "Klub Relax",
            "Erasmus PB — Kurort",
            "ACS",
            "Grant PB — Kasa Studencka",
            "Korty Tenisowe PB",
            "Boisko PB",
            "Sesja poprawkowa",
            "Wydzial Informatyki",
            "Kolokwium — Karta Szansy",
            "Wydzial Mechaniczny",
            "Wydzial Elektryczny",
            "Inno-Eko-Tech — Kurort",
            "CNK",
            "Biblioteka PB",
            "Bistro PB",
            "Radio Akadera",
            "Idz na Warunek",
            "Centrum Sigma",
            "Inkubator Przedsiebiorczosci",
            "Stypendium Rektora — Kasa Studencka",
            "PolitechNET",
            "Aula Duga — Kurort",
            "Kolokwium 2 — Karta Szansy",
            "Wydzial Architektury",
            "Oplata za laby — Podatek",
            "Rektorat"
    };

    public static final String[] TILE_EFFECTS = {
            "Stan na START: bonus okrazenia +300 000 PLN",
            "Nieruchomosc — 60 000 PLN / czynsz od 6 000",
            "Kasa Studencka — stypendium +150 000 PLN",
            "Nieruchomosc — 60 000 PLN / czynsz od 6 000",
            "Podatek 10% od wartosci majatku",
            "Kurort — czynsz 50/100/200/400 tys. PLN",
            "Nieruchomosc — 80 000 PLN / czynsz od 8 000",
            "Losuj karte zdarzenia USOS",
            "Nieruchomosc — 80 000 PLN / czynsz od 8 000",
            "Nieruchomosc — 80 000 PLN / czynsz od 8 000",
            "WARUNEK: lapowka 200 000 PLN",
            "Nieruchomosc — 100 000 PLN / czynsz od 10 000",
            "Gastro — czynsz = suma oczek x 15 000 PLN",
            "Nieruchomosc — 100 000 PLN / czynsz od 10 000",
            "Nieruchomosc — 100 000 PLN / czynsz od 10 000",
            "Kurort — czynsz 50/100/200/400 tys. PLN",
            "Nieruchomosc — 120 000 PLN / czynsz od 12 000",
            "Kasa Studencka — grant +150 000 PLN",
            "Nieruchomosc — 120 000 PLN / czynsz od 12 000",
            "Nieruchomosc — 120 000 PLN / czynsz od 12 000",
            "Sesja poprawkowa — postoj bez oplaty",
            "Nieruchomosc — 160 000 PLN / czynsz od 16 000",
            "Losuj karte zdarzenia — kolokwium",
            "Nieruchomosc — 160 000 PLN / czynsz od 16 000",
            "Nieruchomosc — 160 000 PLN / czynsz od 16 000",
            "Kurort — czynsz 50/100/200/400 tys. PLN",
            "Nieruchomosc — 220 000 PLN / czynsz od 22 000",
            "Nieruchomosc — 220 000 PLN / czynsz od 22 000",
            "Gastro — czynsz = suma oczek x 15 000 PLN",
            "Nieruchomosc — 220 000 PLN / czynsz od 22 000",
            "Idz do Dziekanatu (WARUNEK, poz. 10)",
            "Nieruchomosc — 300 000 PLN / czynsz od 30 000",
            "Nieruchomosc — 300 000 PLN / czynsz od 30 000",
            "Kasa Studencka — stypendium rektora +150 000 PLN",
            "Nieruchomosc — 300 000 PLN / czynsz od 30 000",
            "Kurort — czynsz 50/100/200/400 tys. PLN",
            "Losuj karte zdarzenia — kolokwium 2",
            "Nieruchomosc — 350 000 PLN / czynsz do 1 600 000",
            "Podatek 10% od wartosci majatku",
            "Nieruchomosc — 400 000 PLN / czynsz do 2 000 000"
    };

    public static final int[] TILE_PRICE = GameEconomy.TILE_PRICE;

    public static final int POS_START = GameEconomy.POS_START;
    public static final int POS_JAIL = GameEconomy.POS_JAIL;
    public static final int POS_GO_TO_JAIL = GameEconomy.POS_GO_TO_JAIL;

    private static final boolean[] CHANCE_TILES = new boolean[40];
    static {
        CHANCE_TILES[7] = true;
        CHANCE_TILES[22] = true;
        CHANCE_TILES[36] = true;
    }

    private static final boolean[] RESORT_TILES = GameEconomy.RESORT_TILES;

    private static final boolean[] UTILITY_TILES = GameEconomy.UTILITY_TILES;

    public static final List<ChanceCard> CHANCE_CARDS = List.of(
            new ChanceCard("Stypendium rektora", "Otrzymujesz stypendium naukowe.", 150_000),
            new ChanceCard("Stypendium socjalne", "Komisja stypendialna sie zlitowala.", 100_000),
            new ChanceCard("Wpis warunkowy", "Wykladowca daje warunek za 50 000 PLN.", -50_000),
            new ChanceCard("Mandat za rower na deptaku", "Straz miejska nie spi.", -75_000),
            new ChanceCard("Wygrana w Klubie Gwint", "Wieczor pokerowy w Gwincie konczy sie w bloku.", 200_000),
            new ChanceCard("Awaria kuchenki w akademiku", "Skladasz sie z Kolegami na nowa.", -80_000),
            new ChanceCard("Wsparcie z parafii", "Babcia przyslala wsparcie w kopercie.", 60_000),
            new ChanceCard("Zaliczenie z marszu", "Wykladowca dostal kawe — daje plusa.", 0),
            new ChanceCard("Zwrot z USOSweb", "Pomylka w czesnym, dziekanat zwraca.", 120_000),
            new ChanceCard("Awaria laptopa przed sesja", "Naprawa kosztuje krocie.", -200_000),
            new ChanceCard("Spalony kolokwiom", "Korepetycje przed poprawka.", -120_000),
            new ChanceCard("Wygrana w Konkursie Naukowym", "Twoja praca zwyciezyla na konferencji PB.", 300_000),
            new ChanceCard("Bilet ulgowy z PKP", "Oszczednosc na wyjezdzie do domu.", 30_000),
            new ChanceCard("Zwiedzanie Palacu Branickich", "Bilet studencki gratis, ale zalezles z Patelnia.", -25_000),
            new ChanceCard("Zlapal Cie strażnik biblioteczny", "Zwrot ksiazek po terminie.", -40_000),
            new ChanceCard("Praca dorywcza w stolowce", "Tydzien zmywania naczyn.", 100_000),
            new ChanceCard("Zgubiona legitymacja", "Wyrobienie nowej kosztuje.", -45_000),
            new ChanceCard("Voucher na obiad w stolowce", "Stolowka studencka funduje obiad.", 25_000),
            new ChanceCard("Babcia odwiedzila", "Zostawila siano i sloiki z ogorkami.", 80_000),
            new ChanceCard("Korki ze studentem 5. roku", "Tlumacza Ci algorytmy.", -55_000)
    );

    private static final String[] COLORS = {
            "#38bdf8", "#a855f7", "#10b981", "#f59e0b", "#f43f5e", "#6366f1"
    };

    private final GameSessionRepository sessionRepository;
    private final UserRepository userRepository;
    private final GameSyncService gameSyncService;
    private final MatchHistoryRepository matchHistoryRepository;
    private final OwnedItemRepository ownedItemRepository;
    private final GameInviteRepository gameInviteRepository;
    private final UserNotificationService userNotificationService;
    private final FriendService friendService;
    private BotAutoplayService botAutoplayService;
    private final ModerationService moderationService;

    private final ActiveGameStore activeGameStore;
    private final AchievementService achievementService;

    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager em;

    public GameService(GameSessionRepository sessionRepository,
                       UserRepository userRepository,
                       @Lazy GameSyncService gameSyncService,
                       MatchHistoryRepository matchHistoryRepository,
                       OwnedItemRepository ownedItemRepository,
                       GameInviteRepository gameInviteRepository,
                       UserNotificationService userNotificationService,
                       FriendService friendService,
                       ModerationService moderationService,
                       ActiveGameStore activeGameStore,
                       AchievementService achievementService) {
        this.sessionRepository = sessionRepository;
        this.userRepository = userRepository;
        this.gameSyncService = gameSyncService;
        this.matchHistoryRepository = matchHistoryRepository;
        this.ownedItemRepository = ownedItemRepository;
        this.gameInviteRepository = gameInviteRepository;
        this.userNotificationService = userNotificationService;
        this.friendService = friendService;
        this.moderationService = moderationService;
        this.activeGameStore = activeGameStore;
        this.achievementService = achievementService;
    }

    @org.springframework.beans.factory.annotation.Autowired
    public void setBotAutoplayService(@Lazy BotAutoplayService botAutoplayService) {
        this.botAutoplayService = botAutoplayService;
    }

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
        return sessionRepository.findOpenByUser(username);
    }

    @Transactional
    public GameSession createGame(String creatorUsername, String roomName,
                                  List<Long> friendIds) {
        List<GameSession> mine = myActiveSessions(creatorUsername);
        if (mine.stream().anyMatch(s -> s.getStatus() == GameStatus.ACTIVE)) {
            throw new IllegalArgumentException(
                    "Jestes w trakcie rozgrywki. Wroc do gry lub opusc ja, zanim utworzysz nowy pokoj.");
        }
        User creator = user(creatorUsername);
        moderationService.ensureCanPlay(creator);
        for (GameSession other : mine.stream().filter(s -> s.getStatus() == GameStatus.WAITING).toList()) {
            GamePlayer oldMe = other.getPlayers().stream()
                    .filter(p -> p.getUser() != null && p.getUser().getId().equals(creator.getId()))
                    .findFirst().orElse(null);
            if (oldMe != null) {
                leaveWaitingLobby(other, oldMe);
            }
        }

        GameSession session = new GameSession();
        session.setName(roomName == null || roomName.isBlank() ? "Pokoj " + creatorUsername : roomName.trim());
        session.setCode(generateCode());
        session.setStatus(GameStatus.WAITING);

        session.setLeaderId(creator.getId());
        GamePlayer me = new GamePlayer(creator.getUsername(), uniqueColor(creator, session));
        me.setUser(creator);
        session.addPlayer(me);

        if (friendIds != null) {
            for (Long fid : friendIds) {
                if (session.getPlayers().size() >= MAX_PLAYERS) {
                    break;
                }
                userRepository.findById(fid).ifPresent(friend -> {
                    if (session.getPlayers().size() >= MAX_PLAYERS) {
                        return;
                    }
                    if (moderationService.isUserSuspended(friend)) {
                        return;
                    }
                    if (session.getPlayers().stream().noneMatch(p ->
                            p.getUser() != null && p.getUser().getId().equals(friend.getId()))) {
                        GamePlayer gp = new GamePlayer(friend.getUsername(), uniqueColor(friend, session));
                        gp.setUser(friend);
                        session.addPlayer(gp);
                    }
                });
            }
        }

        for (GamePlayer gp : session.getPlayers()) {
            dealHandCards(gp);
        }

        GameSession saved = persist(session);
        publishPublic(saved.getId(), null, null, null, null, null, null, null);
        return saved;
    }

    private void dealHandCards(GamePlayer player) {
        List<HandCardType> pool = new ArrayList<>(Arrays.asList(HandCardType.values()));
        Collections.shuffle(pool);
        List<String> cards = new ArrayList<>();
        for (int i = 0; i < Math.min(3, pool.size()); i++) {
            cards.add(pool.get(i).name());
        }
        User user = player.getUser();
        if (user != null && user.getStatistics() != null) {
            PlayerStatistics stats = user.getStatistics();
            String wheelCard = stats.getPendingWheelCard();
            if (wheelCard != null) {
                try {
                    HandCardType.valueOf(wheelCard);
                    if (!cards.contains(wheelCard)) {
                        cards.add(wheelCard);
                    }
                } catch (IllegalArgumentException ignored) {

                }
                stats.setPendingWheelCard(null);
            }
            int cashBonus = stats.getPendingStartCashBonus();
            if (cashBonus > 0) {
                player.setCash(player.getCash() + cashBonus);
                stats.setPendingStartCashBonus(0);
            }
        }
        player.setHandCards(cards);
        player.setRollsThisTurn(0);
    }

    @Transactional
    public GameSession joinByCode(String code, String username) {
        GameSession session = sessionRepository.findByCode(code.trim().toUpperCase())
                .orElseThrow(() -> new IllegalArgumentException("Nie ma pokoju o kodzie " + code));
        if (session.getStatus() == GameStatus.FINISHED) {
            throw new IllegalArgumentException("Ta gra zostala juz zakonczona.");
        }
        if (session.getStatus() == GameStatus.ACTIVE) {
            throw new IllegalArgumentException("Gra juz trwa — dolacz przed startem (lobby).");
        }
        User u = user(username);
        moderationService.ensureCanPlay(u);
        if (myActiveSessions(username).stream().anyMatch(s -> s.getStatus() == GameStatus.ACTIVE)) {
            throw new IllegalArgumentException(
                    "Jestes w trakcie rozgrywki. Wroc do gry lub opusc ja, zanim dolaczysz do innego pokoju.");
        }
        for (GameSession other : myActiveSessions(username)) {
            if (other.getStatus() == GameStatus.WAITING && !other.getId().equals(session.getId())) {
                GamePlayer me = other.getPlayers().stream()
                        .filter(p -> p.getUser() != null && p.getUser().getUsername().equals(username))
                        .findFirst().orElse(null);
                if (me != null) {
                    leaveWaitingLobby(other, me);
                }
            }
        }
        boolean already = session.getPlayers().stream()
                .anyMatch(p -> p.getUser() != null && p.getUser().getId().equals(u.getId()));
        if (!already) {
            if (session.getPlayers().size() >= MAX_PLAYERS) {
                throw new IllegalArgumentException("Pokoj jest pelny (max " + MAX_PLAYERS + " graczy).");
            }
            GamePlayer gp = new GamePlayer(u.getUsername(), uniqueColor(u, session));
            gp.setUser(u);
            session.addPlayer(gp);
            persist(session);
            publishPublic(session.getId(), null, null, u.getUsername() + " dolaczyl do gry!", null, null, null, null);
        }
        return session;
    }

    public static final int MAX_PLAYERS = 4;

    @Transactional
    public GameStateDto addBot(Long sessionId, String username, String botName, String botColor) {
        GameSession session = getSession(sessionId);
        if (session.getStatus() != GameStatus.WAITING) {
            throw new IllegalArgumentException("Boty mozna dodawac tylko w lobby.");
        }
        User u = user(username);
        if (!u.getId().equals(session.getLeaderId())) {
            throw new IllegalArgumentException("Tylko lider moze dodawac boty.");
        }
        if (session.getPlayers().size() >= MAX_PLAYERS) {
            throw new IllegalArgumentException("Pokoj jest pelny (max " + MAX_PLAYERS + " graczy).");
        }
        String name = (botName == null || botName.isBlank()) ? nextBotName(session) : botName.trim();
        String color = (botColor == null || botColor.isBlank())
                ? COLORS[session.getPlayers().size() % COLORS.length]
                : botColor.trim();
        session.addPlayer(new GamePlayer(name, color));
        persist(session);
        String msg = name + " dolaczyl do pokoju (bot).";
        publishPublic(sessionId, null, null, msg, null, null, null, null);
        return toState(session, username, null, null, msg, null, null, null, null);
    }

    @Transactional
    public GameStateDto removeBot(Long sessionId, String username, Long botPlayerId) {
        GameSession session = getSession(sessionId);
        if (session.getStatus() != GameStatus.WAITING) {
            throw new IllegalArgumentException("Boty mozna usuwac tylko w lobby.");
        }
        User u = user(username);
        if (!u.getId().equals(session.getLeaderId())) {
            throw new IllegalArgumentException("Tylko lider moze usuwac boty.");
        }
        GamePlayer bot = session.getPlayers().stream()
                .filter(p -> p.getId().equals(botPlayerId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Nie ma takiego gracza w pokoju."));
        if (bot.getUser() != null) {
            throw new IllegalArgumentException("Mozna usunac tylko bota.");
        }
        String name = bot.getDisplayName();
        session.removePlayer(bot);
        persist(session);
        String msg = name + " zostal usuniety z pokoju.";
        publishPublic(sessionId, null, null, msg, null, null, null, null);
        return toState(session, username, null, null, msg, null, null, null, null);
    }

    private String nextBotName(GameSession session) {
        long bots = session.getPlayers().stream().filter(p -> p.getUser() == null).count();
        return "Bot " + (bots + 1);
    }

    @Transactional
    public GameInviteDto inviteFriend(Long sessionId, String username, Long friendUserId) {
        GameSession session = getSession(sessionId);
        if (session.getStatus() != GameStatus.WAITING) {
            throw new IllegalArgumentException("Zaproszenia dzialaja tylko w lobby.");
        }
        User inviter = user(username);
        if (!inviter.getId().equals(session.getLeaderId())) {
            throw new IllegalArgumentException("Tylko lider moze zapraszac graczy.");
        }
        if (session.getPlayers().size() >= MAX_PLAYERS) {
            throw new IllegalArgumentException("Pokoj jest pelny.");
        }
        User invitee = userRepository.findById(friendUserId)
                .orElseThrow(() -> new IllegalArgumentException("Nie ma takiego gracza."));
        if (inviter.getId().equals(invitee.getId())) {
            throw new IllegalArgumentException("Nie mozesz zaprosic samego siebie.");
        }
        if (!friendService.isFriend(username, invitee.getId())) {
            throw new IllegalArgumentException("Mozesz zapraszac tylko znajomych.");
        }
        boolean alreadyIn = session.getPlayers().stream()
                .anyMatch(p -> p.getUser() != null && p.getUser().getId().equals(invitee.getId()));
        if (alreadyIn) {
            throw new IllegalArgumentException("Ten gracz jest juz w pokoju.");
        }
        if (gameInviteRepository.existsBySessionIdAndInviteeIdAndStatus(
                sessionId, invitee.getId(), GameInviteStatus.PENDING)) {
            throw new IllegalArgumentException("Zaproszenie juz wyslane.");
        }
        GameInvite invite = gameInviteRepository.save(new GameInvite(session, inviter, invitee));
        GameInviteDto dto = toInviteDto(invite);
        userNotificationService.sendGameInvite(invitee.getUsername(), GameInviteNotificationDto.of(dto));
        publishPublic(sessionId, null, null,
                inviter.getUsername() + " zaprosil " + invitee.getUsername() + " do pokoju.",
                null, null, null, null);
        return dto;
    }

    @Transactional(readOnly = true)
    public List<GameInviteDto> pendingInvitesFor(String username) {
        User me = user(username);
        return gameInviteRepository.findByInviteeIdAndStatusOrderByCreatedAtDesc(me.getId(), GameInviteStatus.PENDING)
                .stream()
                .filter(inv -> inv.getSession().getStatus() == GameStatus.WAITING)
                .map(this::toInviteDto)
                .toList();
    }

    @Transactional
    public GameSession acceptInvite(Long inviteId, String username) {
        GameInvite invite = gameInviteRepository.findById(inviteId)
                .orElseThrow(() -> new IllegalArgumentException("Brak zaproszenia."));
        if (!invite.getInvitee().getUsername().equals(username)) {
            throw new IllegalArgumentException("To nie Twoje zaproszenie.");
        }
        if (invite.getStatus() != GameInviteStatus.PENDING) {
            throw new IllegalArgumentException("Zaproszenie juz wykorzystane.");
        }
        GameSession session = invite.getSession();
        if (session.getStatus() != GameStatus.WAITING) {
            throw new IllegalArgumentException("Gra juz wystartowala.");
        }
        if (session.getPlayers().size() >= MAX_PLAYERS) {
            throw new IllegalArgumentException("Pokoj jest juz pelny.");
        }
        for (GameSession other : myActiveSessions(username)) {
            if (other.getStatus() == GameStatus.WAITING && !other.getId().equals(session.getId())) {
                GamePlayer me = other.getPlayers().stream()
                        .filter(p -> p.getUser() != null && p.getUser().getUsername().equals(username))
                        .findFirst().orElse(null);
                if (me != null) leaveWaitingLobby(other, me);
            }
        }
        invite.setStatus(GameInviteStatus.ACCEPTED);
        gameInviteRepository.save(invite);
        return joinByCode(session.getCode(), username);
    }

    @Transactional
    public void declineInvite(Long inviteId, String username) {
        GameInvite invite = gameInviteRepository.findById(inviteId)
                .orElseThrow(() -> new IllegalArgumentException("Brak zaproszenia."));
        if (!invite.getInvitee().getUsername().equals(username)) {
            throw new IllegalArgumentException("To nie Twoje zaproszenie.");
        }
        invite.setStatus(GameInviteStatus.DECLINED);
        gameInviteRepository.save(invite);
    }

    private GameInviteDto toInviteDto(GameInvite invite) {
        return new GameInviteDto(
                invite.getId(),
                invite.getSession().getId(),
                invite.getSession().getName(),
                invite.getSession().getCode(),
                invite.getInviter().getUsername(),
                invite.getInviter().getId()
        );
    }

    @Transactional(readOnly = true)
    public GameSession getSession(Long id) {

        GameSession ram = activeGameStore.get(id);
        if (ram != null) return ram;
        return sessionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Nie ma sesji o id " + id));
    }

    private GameSession persist(GameSession session) {
        if (session == null) return null;
        Long id = session.getId();
        if (id != null && activeGameStore.contains(id)) {
            if (session.getStatus() == GameStatus.FINISHED) {
                finalizeFinished(session);
            }
            return session;
        }
        return sessionRepository.save(session);
    }

    private <T> T withLock(Long sessionId, java.util.function.Supplier<T> body) {
        if (sessionId == null) return body.get();
        java.util.concurrent.locks.ReentrantLock lock = activeGameStore.lockFor(sessionId);
        lock.lock();
        try {
            return body.get();
        } finally {
            lock.unlock();
        }
    }

    private void withLock(Long sessionId, Runnable body) {
        withLock(sessionId, () -> { body.run(); return null; });
    }

    private void finalizeFinished(GameSession session) {
        Long id = session.getId();
        em.merge(session);
        activeGameStore.remove(id);
    }

    private void loadIntoRam(GameSession session) {
        for (GamePlayer p : session.getPlayers()) {
            User u = p.getUser();
            if (u != null) {
                u.getId();
                u.getUsername();
            }
        }
        em.flush();
        em.detach(session);
        activeGameStore.put(session);
    }

    @Transactional(readOnly = true)
    public GameStateDto getState(Long sessionId, String username) {
        return toState(getSession(sessionId), username, null, null, null, null, null, null, null);
    }

    @Transactional
    public void endSessionByAdmin(Long sessionId) {
        withLock(sessionId, () -> {
            GameSession ram = activeGameStore.get(sessionId);
            GameSession s = ram != null ? ram : sessionRepository.findById(sessionId).orElse(null);
            if (s == null || s.getStatus() == GameStatus.FINISHED) return;
            s.setStatus(GameStatus.FINISHED);
            if (activeGameStore.contains(sessionId)) {
                finalizeFinished(s);
            } else {
                sessionRepository.save(s);
            }
            publishPublic(sessionId, null, null,
                    "Sesja zostala zakonczona przez administracje.", null, null, null, null);
        });
    }

    @Transactional
    public GameStateDto readyUp(Long sessionId, String username) {
        GameSession session = getSession(sessionId);
        if (session.getStatus() != GameStatus.WAITING) {
            throw new IllegalArgumentException("Gra juz trwa lub zostala zakonczona.");
        }
        ensureLeader(session);
        requireParticipant(session, username);
        GamePlayer me = findPlayerByUsername(session, username);
        me.setReady(!me.isReady());
        persist(session);
        String msg = me.getDisplayName() + (me.isReady() ? " jest gotowy!" : " cofnal gotowosc.");
        publishPublic(sessionId, null, null, msg, null, null, null, null);
        return toState(session, username, null, null, msg, null, null, null, null);
    }

    @Transactional
    public GameStateDto startGame(Long sessionId, String username) {
        GameSession session = getSession(sessionId);
        if (session.getStatus() != GameStatus.WAITING) {
            throw new IllegalArgumentException("Gra juz trwa lub zostala zakonczona.");
        }
        ensureLeader(session);
        User u = user(username);
        if (!u.getId().equals(session.getLeaderId())) {
            throw new IllegalArgumentException("Tylko lider moze rozpoczac gre.");
        }
        boolean allReady = session.getPlayers().stream()
                .filter(p -> p.getUser() != null)
                .allMatch(GamePlayer::isReady);
        if (!allReady) {
            throw new IllegalArgumentException("Nie wszyscy gracze sa gotowi.");
        }
        if (session.getPlayers().size() < 2) {
            throw new IllegalArgumentException("Dodaj co najmniej jednego bota lub poczekaj na gracza.");
        }
        session.setStatus(GameStatus.ACTIVE);
        beginActivePlay(session);
        persist(session);
        loadIntoRam(session);
        String msg = "Gra rozpoczeta! Niech zaczyna sie rozgrywka.";
        publishPublic(sessionId, null, null, msg, null, null, null, null);
        scheduleBotUpdate(sessionId);
        return toState(session, username, null, null, msg, null, null, null, null);
    }

    @Transactional
    public void leaveGame(Long sessionId, String username) {
        withLock(sessionId, () -> leaveGameInternal(sessionId, username));
    }

    private void leaveGameInternal(Long sessionId, String username) {
        GameSession session = getSession(sessionId);
        GamePlayer me = findPlayerByUsername(session, username);

        if (session.getStatus() == GameStatus.WAITING) {
            leaveWaitingLobby(session, me);
            return;
        }

        releaseProperties(me);
        me.setBankrupt(true);
        if (session.getLeaderId() != null && me.getUser() != null
                && session.getLeaderId().equals(me.getUser().getId())) {
            session.getPlayers().stream()
                    .filter(p -> p.getUser() != null && !p.getUser().getId().equals(me.getUser().getId()))
                    .filter(p -> !p.isBankrupt())
                    .findFirst()
                    .ifPresent(next -> session.setLeaderId(next.getUser().getId()));
        }
        StringBuilder msg = new StringBuilder(me.getDisplayName() + " opuszcza gre.");
        checkGameEnd(session, msg);
        persist(session);
        Long sid = session.getId();
        publishPublic(sid, null, null, msg.toString(), null, null, null, null);
        if (session.getStatus() != GameStatus.FINISHED) scheduleBotUpdate(sid);
    }

    private void leaveWaitingLobby(GameSession session, GamePlayer me) {
        if (session.getLeaderId() != null && me.getUser() != null
                && session.getLeaderId().equals(me.getUser().getId())) {
            session.getPlayers().stream()
                    .filter(p -> p.getUser() != null && !p.getUser().getId().equals(me.getUser().getId()))
                    .findFirst()
                    .ifPresentOrElse(
                            next -> session.setLeaderId(next.getUser().getId()),
                            () -> session.setLeaderId(null)
                    );
        }
        session.removePlayer(me);
        long humansLeft = session.getPlayers().stream().filter(p -> p.getUser() != null).count();
        if (humansLeft == 0) {
            session.setStatus(GameStatus.FINISHED);
        }
        persist(session);
        String msg = me.getDisplayName() + " opuscil lobby.";
        publishPublic(session.getId(), null, null, msg, null, null, null, null);
    }

    @Transactional
    public GameStateDto roll(Long sessionId, String username) {
        return withLock(sessionId, () -> rollInternal(sessionId, username));
    }

    private GameStateDto rollInternal(Long sessionId, String username) {
        GameSession session = getSession(sessionId);
        requireParticipant(session, username);
        requireActiveGame(session);
        if (session.getPendingPurchasePos() != null) {
            throw new IllegalArgumentException("Najpierw zakoncz decyzje o polu (kup lub pomin).");
        }
        if (session.getPendingPaymentDebtorId() != null) {
            throw new IllegalArgumentException("Trwa uregulowanie zaleglosci — najpierw oplac lub zbankrutuj.");
        }
        if (session.getPendingUpgradePos() != null) {
            throw new IllegalArgumentException("Najpierw zdecyduj o ulepszeniu pola.");
        }
        if (session.getPendingTakeoverPos() != null) {
            throw new IllegalArgumentException("Najpierw zdecyduj o wykupie dzialki (wykup lub pomin).");
        }
        List<GamePlayer> players = session.getPlayers();
        if (players.isEmpty()) {
            return toState(session, username, null, null, "Brak graczy.", null, null, null, null);
        }

        normalizeCurrentTurn(session);
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
        Long extraRoll = session.getPendingExtraRollPlayerId();
        if (current.getRollsThisTurn() >= 1
                && (extraRoll == null || !extraRoll.equals(current.getId()))) {
            throw new IllegalArgumentException("Juz rzuciles w tej turze. Zakoncz decyzje na planszy.");
        }

        return doRoll(session, current, username);
    }

    @Transactional
    public GameStateDto rollAsBot(Long sessionId, Long botPlayerId) {
        return withLock(sessionId, () -> rollAsBotInternal(sessionId, botPlayerId));
    }

    private GameStateDto rollAsBotInternal(Long sessionId, Long botPlayerId) {
        GameSession session = getSession(sessionId);
        if (session.getStatus() == GameStatus.FINISHED) return null;
        if (session.getPendingPurchasePos() != null) {
            scheduleBotUpdate(sessionId);
            return null;
        }
        if (session.getPendingPaymentDebtorId() != null) {
            scheduleBotUpdate(sessionId);
            return null;
        }
        if (session.getPendingUpgradePos() != null) {
            scheduleBotUpdate(sessionId);
            return null;
        }
        if (session.getPendingTakeoverPos() != null) {
            scheduleBotUpdate(sessionId);
            return null;
        }
        List<GamePlayer> players = session.getPlayers();
        if (players.isEmpty()) return null;
        normalizeCurrentTurn(session);
        GamePlayer current = players.get(session.getCurrentTurn() % players.size());
        if (!current.getId().equals(botPlayerId)) {
            scheduleBotUpdate(sessionId);
            return null;
        }
        if (current.getUser() != null) return null;
        if (current.isBankrupt()) {
            scheduleBotUpdate(sessionId);
            return null;
        }
        if (current.getCash() < 0) {
            scheduleBotUpdate(sessionId);
            return null;
        }
        Long extraRoll = session.getPendingExtraRollPlayerId();
        if (current.getRollsThisTurn() >= 1
                && (extraRoll == null || !extraRoll.equals(current.getId()))) {
            scheduleBotUpdate(sessionId);
            return null;
        }
        return doRoll(session, current, null);
    }

    private GameStateDto doRoll(GameSession session, GamePlayer current, String username) {
        if (session.getPendingExtraRollPlayerId() != null
                && session.getPendingExtraRollPlayerId().equals(current.getId())
                && current.getRollsThisTurn() >= 1) {
            session.setPendingExtraRollPlayerId(null);
        }

        int d1 = ThreadLocalRandom.current().nextInt(1, 7);
        int d2 = ThreadLocalRandom.current().nextInt(1, 7);
        int steps = d1 + d2;
        boolean isDoubles = (d1 == d2);

        int oldPos = current.getPosition();
        StringBuilder msg = new StringBuilder();
        msg.append(current.getDisplayName()).append(" rzuca ")
                .append(d1).append("+").append(d2).append("=").append(steps)
                .append(isDoubles ? " (DUBLET!). " : ". ");

        boolean jailedByDoubles = false;
        if (isDoubles) {
            current.setDoublesCount(current.getDoublesCount() + 1);
            if (current.getDoublesCount() >= 3) {
                jailedByDoubles = true;
            }
        } else {
            current.setDoublesCount(0);
        }

        int newPos;
        ChanceCard drawnCard = null;

        if (jailedByDoubles) {

            newPos = POS_JAIL;
            current.setDoublesCount(0);
            current.setPosition(newPos);
            msg.append("Trzeci DUBLET z rzedu — idziesz prosto do Dziekanatu (WIEZIENIE)! ");
        } else {
            newPos = (oldPos + steps) % 40;

            if (oldPos + steps >= 40) {
                current.setCash(current.getCash() + GameEconomy.GO_BONUS);
                msg.append("Przejscie przez START (+").append(GameEconomy.GO_BONUS).append(" PLN). ");
            }

            if (newPos == POS_GO_TO_JAIL) {
                newPos = POS_JAIL;
                current.setDoublesCount(0);
                msg.append("Trafia na pole \"Idz do Dziekanatu\" - laduje w WIEZIENIU! ");
            }

            current.setPosition(newPos);
            msg.append("Staje na: ").append(TILES[newPos]).append(". ");

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
                                .append(" PLN, lub pominac. ");
                    } else {
                        msg.append("Za malo siana, by kupic - pole zostaje wolne. ");
                    }
                } else if (!ownerOfTile.getId().equals(current.getId()) && !ownerOfTile.isBankrupt()) {

                    if (current.isSkipNextRent()) {
                        current.setSkipNextRent(false);
                        msg.append("Karta Ochrony! ").append(current.getDisplayName())
                                .append(" omija czynsz na ").append(TILES[newPos]).append(". ");
                        if (ownerOfTile.isDoubleRentNext()) {
                            ownerOfTile.setDoubleRentNext(false);
                        }
                    } else {
                        int rent = computeRent(session, ownerOfTile, newPos, steps);

                        boolean doubled = ownerOfTile.isDoubleRentNext();
                        if (doubled) {
                            rent *= 2;
                            ownerOfTile.setDoubleRentNext(false);
                        }
                        if (chargePlayer(session, current, rent, ownerOfTile,
                                "Czynsz: " + TILES[newPos], msg)) {
                            msg.append(current.getDisplayName()).append(" placi czynsz ")
                                    .append(rent).append(" PLN dla ")
                                    .append(ownerOfTile.getDisplayName())
                                    .append(doubled ? " (PODWOJONY)! " : ". ");

                            offerTakeoverIfPossible(session, current, ownerOfTile, newPos, msg);
                        }
                    }
                } else if (ownerOfTile.getId().equals(current.getId())) {
                    msg.append("To Twoje pole - bez czynszu. ");

                    if (!RESORT_TILES[newPos] && !UTILITY_TILES[newPos] && !CHANCE_TILES[newPos]) {
                        int level = current.getPropertyLevels().getOrDefault(newPos, 0);
                        int upgradeCost = GameEconomy.upgradeCost(newPos);
                        if (level < GameEconomy.MAX_PROPERTY_LEVEL
                                && upgradeCost > 0
                                && session.getPendingUpgradePos() == null) {
                            session.setPendingUpgradePos(newPos);
                            session.setPendingUpgradePlayerId(current.getId());
                            session.setPendingUpgradeCost(upgradeCost);
                            int newRent = computeRentForLevel(newPos, level + 1, current);
                            String nextLabel = upgradeOfferLabel(level);
                            msg.append("Mozesz ulepszyc swoje pole! Zbuduj ")
                                    .append(nextLabel)
                                    .append(" za ").append(upgradeCost)
                                    .append(" PLN (nowy czynsz: ").append(newRent).append(" PLN). ");
                        }
                    }
                }
            }
        }

        if (isDoubles && !jailedByDoubles && !current.isBankrupt()) {
            session.setPendingExtraRollPlayerId(current.getId());
        }

        if (session.getPendingPurchasePos() == null
                && session.getPendingPaymentDebtorId() == null
                && session.getPendingUpgradePos() == null
                && session.getPendingBuybackVictimId() == null
                && session.getPendingTakeoverPos() == null) {
            endTurnOrExtraRoll(session, current);
        }
        current.setRollsThisTurn(current.getRollsThisTurn() + 1);
        checkGameEnd(session, msg);
        if (session.getStatus() != GameStatus.FINISHED) {
            appendWinAlerts(session.getPlayers(), msg);
        }
        persist(session);

        Long movedId = current.getId();
        ChanceCardDto cardDto = drawnCard != null
                ? new ChanceCardDto(drawnCard.title(), drawnCard.description(), drawnCard.moneyEffect())
                : null;
        Long sessionId = session.getId();
        publishPublic(sessionId, d1, d2, msg.toString(), movedId, oldPos, newPos, cardDto);

        scheduleBotUpdate(sessionId);
        return toState(session, username, d1, d2, msg.toString(), movedId, oldPos, newPos, cardDto);
    }

    @Transactional
    public GameStateDto buy(Long sessionId, String username) {
        return withLock(sessionId, () -> buyInternal(sessionId, username));
    }

    private GameStateDto buyInternal(Long sessionId, String username) {
        GameSession session = getSession(sessionId);
        requireParticipant(session, username);
        requireActiveGame(session);
        if (session.getPendingPurchasePos() == null) {
            throw new IllegalArgumentException("Brak aktywnego pola do kupna.");
        }
        GamePlayer me = findPlayerByUsername(session, username);
        if (!me.getId().equals(session.getPendingDeciderId())) {
            throw new IllegalArgumentException("To nie Ty stoisz na tym polu.");
        }
        return doBuy(session, me, username);
    }

    @Transactional
    public GameStateDto buyAsBot(Long sessionId, Long botPlayerId) {
        return withLock(sessionId, () -> buyAsBotInternal(sessionId, botPlayerId));
    }

    private GameStateDto buyAsBotInternal(Long sessionId, Long botPlayerId) {
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
        Integer posObj = session.getPendingPurchasePos();
        if (posObj == null) {
            throw new IllegalArgumentException("Brak aktywnego pola do kupna.");
        }
        int pos = posObj;
        int price = resolvePurchasePrice(session, pos);
        if (me.getCash() < price) {
            throw new IllegalArgumentException("Za malo siana na zakup.");
        }
        me.setCash(me.getCash() - price);
        me.getOwnedPositions().add(pos);

        if (GameEconomy.upgradeCost(pos) > 0) {
            me.getPropertyLevels().put(pos, 1);
        }
        session.clearPendingPurchase();
        endTurnOrExtraRoll(session, me);

        StringBuilder msg = new StringBuilder();
        msg.append(me.getDisplayName()).append(" kupuje ").append(TILES[pos])
                .append(" za ").append(price).append(" PLN (z 1 domem). ");

        if (GameEconomy.hasColorMonopoly(me, pos)) {
            msg.append("[MONOPOL] ").append(me.getDisplayName()).append(" zdobywa monopol na grupie! ");
        }
        checkGameEnd(session, msg);
        if (session.getStatus() != GameStatus.FINISHED) {
            appendWinAlerts(session.getPlayers(), msg);
        }
        persist(session);

        Long sessionId = session.getId();
        publishPublic(sessionId, null, null, msg.toString(), null, null, null, null);
        scheduleBotUpdate(sessionId);
        return toState(session, username, null, null, msg.toString(), null, null, null, null);
    }

    @Transactional
    public GameStateDto skipPurchase(Long sessionId, String username) {
        return withLock(sessionId, () -> skipPurchaseInternal(sessionId, username));
    }

    private GameStateDto skipPurchaseInternal(Long sessionId, String username) {
        GameSession session = getSession(sessionId);
        requireParticipant(session, username);
        requireActiveGame(session);
        if (session.getPendingPurchasePos() == null) {
            throw new IllegalArgumentException("Brak aktywnego pola.");
        }
        GamePlayer me = findPlayerByUsername(session, username);
        if (!me.getId().equals(session.getPendingDeciderId())) {
            throw new IllegalArgumentException("To nie Ty mozesz pominac.");
        }
        return doSkip(session, me, username, false);
    }

    @Transactional
    public GameStateDto skipAsBot(Long sessionId, Long botPlayerId) {
        return withLock(sessionId, () -> skipAsBotInternal(sessionId, botPlayerId));
    }

    private GameStateDto skipAsBotInternal(Long sessionId, Long botPlayerId) {
        GameSession session = getSession(sessionId);
        if (session.getPendingPurchasePos() == null) return null;
        if (!botPlayerId.equals(session.getPendingDeciderId())) return null;
        GamePlayer me = session.getPlayers().stream()
                .filter(p -> p.getId().equals(botPlayerId))
                .findFirst().orElse(null);
        if (me == null) return null;
        return doSkip(session, me, null, false);
    }

    @Transactional
    public GameStateDto botDecide(Long sessionId, Long botPlayerId, int snapPos) {
        return withLock(sessionId, () -> botDecideInternal(sessionId, botPlayerId, snapPos));
    }

    private GameStateDto botDecideInternal(Long sessionId, Long botPlayerId, int snapPos) {
        GameSession session = getSession(sessionId);
        if (session.getPendingPurchasePos() == null) {
            scheduleBotUpdate(sessionId);
            return null;
        }
        if (session.getPendingPurchasePos() != snapPos) {
            scheduleBotUpdate(sessionId);
            return null;
        }
        if (!botPlayerId.equals(session.getPendingDeciderId())) {
            scheduleBotUpdate(sessionId);
            return null;
        }
        GamePlayer bot = session.getPlayers().stream()
                .filter(p -> p.getId().equals(botPlayerId))
                .findFirst().orElse(null);
        if (bot == null || bot.getUser() != null) {
            scheduleBotUpdate(sessionId);
            return null;
        }
        int price = session.getPendingPurchasePrice() != null ? session.getPendingPurchasePrice() : 0;
        if (bot.getCash() >= price * 2) {
            return doBuy(session, bot, null);
        }
        return doSkip(session, bot, null, false);
    }

    @Transactional
    public GameStateDto botUpgradeDecide(Long sessionId, Long botPlayerId, int snapPos) {
        return withLock(sessionId, () -> botUpgradeDecideInternal(sessionId, botPlayerId, snapPos));
    }

    private GameStateDto botUpgradeDecideInternal(Long sessionId, Long botPlayerId, int snapPos) {
        GameSession session = getSession(sessionId);
        if (session.getPendingUpgradePos() == null) {
            scheduleBotUpdate(sessionId);
            return null;
        }
        if (session.getPendingUpgradePos() != snapPos) {
            scheduleBotUpdate(sessionId);
            return null;
        }
        if (!botPlayerId.equals(session.getPendingUpgradePlayerId())) {
            scheduleBotUpdate(sessionId);
            return null;
        }
        GamePlayer bot = session.getPlayers().stream()
                .filter(p -> p.getId().equals(botPlayerId))
                .findFirst().orElse(null);
        if (bot == null || bot.getUser() != null) {
            scheduleBotUpdate(sessionId);
            return null;
        }
        int cost = session.getPendingUpgradeCost() != null ? session.getPendingUpgradeCost() : 0;
        if (bot.getCash() >= cost * 2) {
            return doUpgrade(session, bot, null);
        }
        return doSkipUpgrade(session, bot, null, false);
    }

    @Transactional
    public GameStateDto autoSkipTimeout(Long sessionId, int snapPosition) {
        return withLock(sessionId, () -> autoSkipTimeoutInternal(sessionId, snapPosition));
    }

    private GameStateDto autoSkipTimeoutInternal(Long sessionId, int snapPosition) {
        GameSession session = getSession(sessionId);
        if (session.getPendingPurchasePos() == null) {
            scheduleBotUpdate(sessionId);
            return null;
        }
        if (session.getPendingPurchasePos() != snapPosition) {
            scheduleBotUpdate(sessionId);
            return null;
        }
        Long deciderId = session.getPendingDeciderId();
        if (deciderId == null) {
            scheduleBotUpdate(sessionId);
            return null;
        }
        GamePlayer me = session.getPlayers().stream()
                .filter(p -> p.getId().equals(deciderId))
                .findFirst().orElse(null);
        if (me == null) {
            scheduleBotUpdate(sessionId);
            return null;
        }
        return doSkip(session, me, null, true);
    }

    private GameStateDto doSkip(GameSession session, GamePlayer me, String username, boolean timeout) {
        int pos = session.getPendingPurchasePos();
        session.clearPendingPurchase();
        endTurnOrExtraRoll(session, me);
        persist(session);

        String msg = timeout
                ? me.getDisplayName() + " nie zdecydowal sie w czasie - pole " + TILES[pos] + " zostaje wolne."
                : me.getDisplayName() + " pomija pole " + TILES[pos] + " - zostaje wolne.";
        Long sessionId = session.getId();
        publishPublic(sessionId, null, null, msg, null, null, null, null);
        scheduleBotUpdate(sessionId);
        return toState(session, username, null, null, msg, null, null, null, null);
    }

    @Transactional
    public GameStateDto bid(Long sessionId, String username, int amount) {
        return withLock(sessionId, () -> bidInternal(sessionId, username, amount));
    }

    private GameStateDto bidInternal(Long sessionId, String username, int amount) {
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

        session.setPendingExtraRollPlayerId(null);
        advanceTurn(session);
        persist(session);

        String msg = me.getDisplayName() + " wygrywa licytacje na " + TILES[pos] + " za " + amount + " PLN!";
        publishPublic(sessionId, null, null, msg, null, null, null, null);
        scheduleBotUpdate(sessionId);
        return toState(session, username, null, null, msg, null, null, null, null);
    }

    @Transactional
    public GameStateDto transfer(Long sessionId, String username, Long toPlayerId, int amount) {
        return withLock(sessionId, () -> transferInternal(sessionId, username, toPlayerId, amount));
    }

    private GameStateDto transferInternal(Long sessionId, String username, Long toPlayerId, int amount) {
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
        persist(session);

        String msg = from.getDisplayName() + " przelal " + amount + " PLN do " + to.getDisplayName() + ".";
        if (session.getPendingPaymentDebtorId() != null
                && session.getPendingPaymentDebtorId().equals(to.getId())) {
            msg += " (pozyczka na splate zadluzenia)";
        }
        publishPublic(sessionId, null, null, msg, null, null, null, null);
        return toState(session, username, null, null, msg, null, null, null, null);
    }

    @Transactional
    public GameStateDto sellProperty(Long sessionId, String username, int position) {
        return withLock(sessionId, () -> sellPropertyInternal(sessionId, username, position));
    }

    private GameStateDto sellPropertyInternal(Long sessionId, String username, int position) {
        GameSession session = getSession(sessionId);
        requireActiveGame(session);
        GamePlayer me = findPlayerByUsername(session, username);
        if (!me.getOwnedPositions().contains(position)) {
            throw new IllegalArgumentException("Nie posiadasz tego pola.");
        }
        String msg = doSellProperty(session, me, position);
        persist(session);
        Long sessionIdVal = session.getId();
        publishPublic(sessionIdVal, null, null, msg, null, null, null, null);
        scheduleBotUpdate(sessionIdVal);
        return toState(session, username, null, null, msg, null, null, null, null);
    }

    @Transactional
    public GameStateDto payDebt(Long sessionId, String username) {
        return withLock(sessionId, () -> payDebtInternal(sessionId, username));
    }

    private GameStateDto payDebtInternal(Long sessionId, String username) {
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

    @Transactional
    public GameStateDto declareBankruptcy(Long sessionId, String username) {
        return withLock(sessionId, () -> declareBankruptcyInternal(sessionId, username));
    }

    private GameStateDto declareBankruptcyInternal(Long sessionId, String username) {
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

    @Transactional
    public GameStateDto resolvePaymentAsBot(Long sessionId, Long debtorId, int snapAmount) {
        return withLock(sessionId, () -> resolvePaymentAsBotInternal(sessionId, debtorId, snapAmount));
    }

    private GameStateDto resolvePaymentAsBotInternal(Long sessionId, Long debtorId, int snapAmount) {
        GameSession session = getSession(sessionId);
        if (session.getPendingPaymentDebtorId() == null) {
            scheduleBotUpdate(sessionId);
            return null;
        }
        if (!debtorId.equals(session.getPendingPaymentDebtorId())) {
            scheduleBotUpdate(sessionId);
            return null;
        }
        if (session.getPendingPaymentAmount() == null || session.getPendingPaymentAmount() != snapAmount) {
            scheduleBotUpdate(sessionId);
            return null;
        }
        GamePlayer debtor = session.getPlayers().stream()
                .filter(p -> p.getId().equals(debtorId))
                .findFirst().orElse(null);
        if (debtor == null || debtor.isBankrupt()) {
            scheduleBotUpdate(sessionId);
            return null;
        }

        StringBuilder msg = new StringBuilder();
        while (debtor.getCash() < snapAmount && !debtor.getOwnedPositions().isEmpty()) {
            int cheapest = cheapestOwnedPosition(debtor);
            msg.append(doSellProperty(session, debtor, cheapest)).append(" ");
        }
        if (debtor.getCash() >= snapAmount) {
            if (!msg.isEmpty()) publishPublic(sessionId, null, null, msg.toString(), null, null, null, null);
            return doPayDebt(session, debtor, null);
        }
        if (!msg.isEmpty()) publishPublic(sessionId, null, null, msg.toString(), null, null, null, null);
        return doDeclareBankruptcy(session, debtor, null);
    }

    @Transactional
    public GameStateDto autoPaymentTimeout(Long sessionId, int snapAmount) {
        return withLock(sessionId, () -> autoPaymentTimeoutInternal(sessionId, snapAmount));
    }

    private GameStateDto autoPaymentTimeoutInternal(Long sessionId, int snapAmount) {
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
            case POS_START -> msg.append("STAN NA START — witaj na linii startu! ");
            case 4, 38 -> {
                int tax = GameEconomy.computeTax(player);
                if (tax > 0) {
                    chargePlayer(session, player, tax, null, "Podatek od majatku", msg);
                    msg.append("Podatek 10% od majatku (-").append(tax).append(" PLN). ");
                } else {
                    msg.append("Podatek 10% od majatku — brak nieruchomosci, bez oplaty. ");
                }
            }
            case POS_JAIL -> {
                if (player.isJailPassActive()) {
                    player.setJailPassActive(false);
                    msg.append("Przepustka z Dziekanatu! Brak oplaty za wizyte w Dziekanacie. ");
                } else if (chargePlayer(session, player, GameEconomy.JAIL_BAIL, null, "Lapowka w Dziekanacie", msg)) {
                    msg.append("Dziekanat (wiezienie): lapowka ").append(GameEconomy.JAIL_BAIL).append(" PLN. ");
                } else {
                    msg.append("Dziekanat (wiezienie): lapowka ").append(GameEconomy.JAIL_BAIL)
                            .append(" PLN — brak siana! ");
                }
            }
            case 2, 17, 33 -> {
                player.setCash(player.getCash() + GameEconomy.SCHOLARSHIP_BONUS);
                msg.append("Kasa Studencka — stypendium (+").append(GameEconomy.SCHOLARSHIP_BONUS).append(" PLN). ");
            }
            default -> {  }
        }
    }

    private ChanceCard drawCard() {
        return CHANCE_CARDS.get(ThreadLocalRandom.current().nextInt(CHANCE_CARDS.size()));
    }

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

    private int computeRent(GameSession session, GamePlayer owner, int pos, int diceSum) {
        if (RESORT_TILES[pos]) {
            int resorts = 0;
            for (int i = 0; i < 40; i++) {
                if (RESORT_TILES[i] && owner.getOwnedPositions().contains(i)) resorts++;
            }
            return GameEconomy.RESORT_RENT[Math.min(resorts, GameEconomy.RESORT_RENT.length - 1)];
        }
        if (UTILITY_TILES[pos]) {
            return diceSum * GameEconomy.UTILITY_RENT_MULTIPLIER;
        }
        int level = owner.getPropertyLevels().getOrDefault(pos, 0);
        boolean monopoly = level == 0 && GameEconomy.hasColorMonopoly(owner, pos);
        return computeRentForLevel(pos, level, monopoly);
    }

    static int computeRentForLevel(int pos, int level, boolean monopoly) {
        return GameEconomy.rentForLevel(pos, level, monopoly);
    }

    static int computeRentForLevel(int pos, int level, GamePlayer owner) {
        boolean monopoly = level == 0 && owner != null && GameEconomy.hasColorMonopoly(owner, pos);
        return computeRentForLevel(pos, level, monopoly);
    }

    private static String upgradeOfferLabel(int currentLevel) {
        return switch (currentLevel) {
            case 0 -> "1 dom";
            case 1 -> "2 domy";
            case 2 -> "3 domy";
            case 3 -> "Biurowiec";
            default -> "ulepszenie";
        };
    }

    private static String propertyLevelLabel(int level) {
        if (level <= 0) {
            return "Sam grunt";
        }
        return upgradeBuiltLabel(level);
    }

    private static String upgradeBuiltLabel(int newLevel) {
        return switch (newLevel) {
            case 1 -> "1 dom";
            case 2 -> "2 domy";
            case 3 -> "3 domy";
            case 4 -> "Biurowiec";
            default -> "ulepszenie";
        };
    }

    static int upgradeCost(int pos) {
        return GameEconomy.upgradeCost(pos);
    }

    private void releaseProperties(GamePlayer player) {
        player.getOwnedPositions().clear();
        player.getPropertyLevels().clear();
        player.getLandingCounts().clear();
    }

    private static void clearPropertyDevelopment(GamePlayer player, int position) {
        player.getPropertyLevels().remove(position);
        player.getLandingCounts().remove(position);
    }

    private void requireActiveGame(GameSession session) {
        if (session.getStatus() == GameStatus.WAITING) {
            throw new IllegalArgumentException("Gra nie zostala jeszcze rozpoczeta przez lidera.");
        }
        if (session.getStatus() == GameStatus.FINISHED) {
            throw new IllegalArgumentException("Gra zostala zakonczona.");
        }
    }

    public static int sellPrice(int position) {
        return GameEconomy.sellPrice(position);
    }

    private boolean chargePlayer(GameSession session, GamePlayer debtor, int amount,
                                 GamePlayer creditor, String reason, StringBuilder msg) {
        if (amount <= 0) return true;

        if (debtor.isShieldActive() && amount <= GameEconomy.SHIELD_MAX_ABSORB) {
            debtor.setShieldActive(false);
            msg.append("Tarcza Akademicka absorbuje oplate ").append(amount).append(" PLN! ");
            if (creditor != null) creditor.setCash(creditor.getCash() + amount);
            return true;
        }

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

    private String doSellProperty(GameSession session, GamePlayer player, int position) {
        if (!player.getOwnedPositions().contains(position)) {
            throw new IllegalArgumentException("Nie posiadasz tego pola.");
        }
        int salePrice = sellPrice(position);
        if (salePrice <= 0) {
            throw new IllegalArgumentException("To pole nie jest na sprzedaz.");
        }
        player.getOwnedPositions().remove(position);
        clearPropertyDevelopment(player, position);
        player.setCash(player.getCash() + salePrice);
        if (session != null) {
            if (Integer.valueOf(position).equals(session.getPendingUpgradePos())) {
                session.clearPendingUpgrade();
            }
            if (Integer.valueOf(position).equals(session.getPendingPurchasePos())) {
                session.clearPendingPurchase();
            }
            if (Integer.valueOf(position).equals(session.getPendingBuybackPos())) {
                session.clearPendingBuyback();
            }
        }
        return player.getDisplayName() + " sprzedaje " + TILES[position]
                + " do banku za " + salePrice + " PLN (70% ceny gruntu).";
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
        boolean upgradeAfterPay = session.getPendingUpgradePos() != null
                && session.getPendingUpgradePlayerId() != null
                && session.getPendingUpgradePlayerId().equals(debtor.getId());
        session.clearPendingPayment();
        if (upgradeAfterPay) {
            StringBuilder msg = new StringBuilder(debtor.getDisplayName() + " oplaca " + amount + " PLN (" + reason + "). ");
            persist(session);
            publishPublic(session.getId(), null, null, msg.toString(), null, null, null, null);
            return doUpgrade(session, debtor, username);
        }
        StringBuilder msg = new StringBuilder(debtor.getDisplayName() + " oplaca " + amount + " PLN (" + reason + "). ");
        endTurnOrExtraRoll(session, debtor);
        checkGameEnd(session, msg);
        persist(session);
        Long sessionId = session.getId();
        publishPublic(sessionId, null, null, msg.toString(), null, null, null, null);
        scheduleBotUpdate(sessionId);
        return toState(session, username, null, null, msg.toString(), null, null, null, null);
    }

    private GameStateDto doDeclareBankruptcy(GameSession session, GamePlayer debtor, String username) {
        StringBuilder msg = new StringBuilder();
        applyBankruptcy(debtor, msg);
        session.clearPendingPayment();

        session.setPendingExtraRollPlayerId(null);
        advanceTurn(session);
        checkGameEnd(session, msg);
        persist(session);
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

    private void offerTakeoverIfPossible(GameSession session, GamePlayer buyer,
                                         GamePlayer seller, int pos, StringBuilder msg) {
        if (TILE_PRICE[pos] <= 0) return;
        if (RESORT_TILES[pos] || UTILITY_TILES[pos] || CHANCE_TILES[pos]) return;
        if (seller == null || seller.isBankrupt() || buyer.getId().equals(seller.getId())) return;
        if (session.getPendingPurchasePos() != null
                || session.getPendingPaymentDebtorId() != null
                || session.getPendingUpgradePos() != null
                || session.getPendingBuybackVictimId() != null
                || session.getPendingTakeoverPos() != null) return;
        int level = seller.getPropertyLevels().getOrDefault(pos, 0);
        int price = GameEconomy.buyoutPrice(pos, level);
        if (price <= 0 || buyer.getCash() < price) return;
        session.setPendingTakeoverPos(pos);
        session.setPendingTakeoverBuyerId(buyer.getId());
        session.setPendingTakeoverSellerId(seller.getId());
        session.setPendingTakeoverPrice(price);
        msg.append("Mozesz wykupic ").append(TILES[pos]).append(" od ")
                .append(seller.getDisplayName()).append(" za ").append(price).append(" PLN! ");
    }

    @Transactional
    public GameStateDto buyoutTakeover(Long sessionId, String username) {
        return withLock(sessionId, () -> {
            GameSession session = getSession(sessionId);
            requireParticipant(session, username);
            requireActiveGame(session);
            if (session.getPendingTakeoverPos() == null) {
                throw new IllegalArgumentException("Brak aktywnego wykupu dzialki.");
            }
            GamePlayer me = findPlayerByUsername(session, username);
            if (!me.getId().equals(session.getPendingTakeoverBuyerId())) {
                throw new IllegalArgumentException("To nie Ty mozesz wykupic to pole.");
            }
            return doTakeover(session, me, username);
        });
    }

    @Transactional
    public GameStateDto skipTakeover(Long sessionId, String username) {
        return withLock(sessionId, () -> {
            GameSession session = getSession(sessionId);
            requireParticipant(session, username);
            requireActiveGame(session);
            if (session.getPendingTakeoverPos() == null) {
                throw new IllegalArgumentException("Brak aktywnego wykupu dzialki.");
            }
            GamePlayer me = findPlayerByUsername(session, username);
            if (!me.getId().equals(session.getPendingTakeoverBuyerId())) {
                throw new IllegalArgumentException("To nie Twoja decyzja o wykupie.");
            }
            return doSkipTakeover(session, me, username);
        });
    }

    @Transactional
    public GameStateDto autoTakeoverTimeout(Long sessionId, int snapPos) {
        return withLock(sessionId, () -> {
            GameSession session = getSession(sessionId);
            if (session.getPendingTakeoverPos() == null
                    || !Integer.valueOf(snapPos).equals(session.getPendingTakeoverPos())) {
                return null;
            }
            GamePlayer buyer = session.getPlayers().stream()
                    .filter(p -> p.getId().equals(session.getPendingTakeoverBuyerId()))
                    .findFirst().orElse(null);
            if (buyer == null) { session.clearPendingTakeover(); return null; }
            return doSkipTakeover(session, buyer, null);
        });
    }

    @Transactional
    public GameStateDto resolveTakeoverAsBot(Long sessionId, Long buyerId, int snapPos) {
        return withLock(sessionId, () -> {
            GameSession session = getSession(sessionId);
            if (session.getPendingTakeoverPos() == null
                    || !Integer.valueOf(snapPos).equals(session.getPendingTakeoverPos())) {
                return null;
            }
            if (!buyerId.equals(session.getPendingTakeoverBuyerId())) return null;
            GamePlayer buyer = session.getPlayers().stream()
                    .filter(p -> p.getId().equals(buyerId))
                    .findFirst().orElse(null);
            if (buyer == null || buyer.getUser() != null) return null;
            int price = session.getPendingTakeoverPrice() != null ? session.getPendingTakeoverPrice() : 0;
            if (price > 0 && buyer.getCash() >= (long) price * 3 / 2) {
                return doTakeover(session, buyer, null);
            }
            return doSkipTakeover(session, buyer, null);
        });
    }

    private GameStateDto doTakeover(GameSession session, GamePlayer buyer, String username) {
        int pos = session.getPendingTakeoverPos();
        int price = session.getPendingTakeoverPrice() != null ? session.getPendingTakeoverPrice() : 0;
        GamePlayer seller = session.getPlayers().stream()
                .filter(p -> p.getId().equals(session.getPendingTakeoverSellerId()))
                .findFirst().orElse(null);
        StringBuilder msg = new StringBuilder();
        if (buyer.getCash() < price || price <= 0) {
            session.clearPendingTakeover();
            msg.append("Za malo siana na wykup — pole zostaje u wlasciciela. ");
        } else if (seller == null || seller.isBankrupt() || !seller.getOwnedPositions().contains(pos)) {
            session.clearPendingTakeover();
            msg.append("Wykup niemozliwy — pole zmienilo wlasciciela. ");
        } else {
            buyer.setCash(buyer.getCash() - price);
            seller.setCash(seller.getCash() + price);
            seller.getOwnedPositions().remove(pos);
            clearPropertyDevelopment(seller, pos);
            buyer.getOwnedPositions().add(pos);
            session.clearPendingTakeover();
            msg.append(buyer.getDisplayName()).append(" WYKUPUJE ").append(TILES[pos])
                    .append(" od ").append(seller.getDisplayName())
                    .append(" za ").append(price).append(" PLN! ");
        }
        return finishTakeover(session, buyer, username, msg);
    }

    private GameStateDto doSkipTakeover(GameSession session, GamePlayer buyer, String username) {
        session.clearPendingTakeover();
        StringBuilder msg = new StringBuilder(buyer.getDisplayName() + " rezygnuje z wykupu dzialki. ");
        return finishTakeover(session, buyer, username, msg);
    }

    private GameStateDto finishTakeover(GameSession session, GamePlayer buyer, String username, StringBuilder msg) {
        endTurnOrExtraRoll(session, buyer);
        checkGameEnd(session, msg);
        if (session.getStatus() != GameStatus.FINISHED) {
            appendWinAlerts(session.getPlayers(), msg);
        }
        persist(session);
        Long sessionId = session.getId();
        publishPublic(sessionId, null, null, msg.toString(), null, null, null, null);
        scheduleBotUpdate(sessionId);
        return toState(session, username, null, null, msg.toString(), null, null, null, null);
    }

    @org.springframework.scheduling.annotation.Scheduled(fixedDelay = 30_000L)
    @Transactional
    public void enforceTimeLimits() {

        for (GameSession ref : new ArrayList<>(activeGameStore.activeSessions())) {
            Long id = ref.getId();
            withLock(id, () -> {
                GameSession s = activeGameStore.get(id);
                if (s == null || s.getStatus() != GameStatus.ACTIVE || s.getCreatedAt() == null) return;
                long elapsed = ChronoUnit.SECONDS.between(s.getCreatedAt(), LocalDateTime.now());
                if (elapsed < GameEconomy.GAME_DURATION_SECONDS) return;
                StringBuilder msg = new StringBuilder();
                checkGameEnd(s, msg);
                if (s.getStatus() == GameStatus.FINISHED) {
                    finalizeFinished(s);
                    publishPublic(id, null, null, msg.toString(), null, null, null, null);
                }
            });
        }
    }

    private void checkGameEnd(GameSession session, StringBuilder msg) {
        if (session.getStatus() == GameStatus.FINISHED) return;
        if (session.getStatus() == GameStatus.WAITING) return;
        List<GamePlayer> active = session.getPlayers().stream()
                .filter(p -> !p.isBankrupt())
                .toList();

        if (session.getCreatedAt() != null && !active.isEmpty()) {
            long elapsed = ChronoUnit.SECONDS.between(session.getCreatedAt(), LocalDateTime.now());
            if (elapsed >= GameEconomy.GAME_DURATION_SECONDS) {
                GamePlayer richest = active.stream()
                        .max(java.util.Comparator.comparingInt(GameEconomy::netWorth))
                        .orElse(active.get(0));
                session.setStatus(GameStatus.FINISHED);
                msg.append("Czas minal (60 min)! ").append(richest.getDisplayName())
                        .append(" WYGRYWA z najwyzszym majatkiem (").append(GameEconomy.netWorth(richest))
                        .append(" PLN)! ");
                persistGameResults(session, richest);
                return;
            }
        }

        for (GamePlayer p : active) {
            if (countColorMonopolies(p) >= 3) {
                session.setStatus(GameStatus.FINISHED);
                msg.append(p.getDisplayName()).append(" WYGRYWA przez MONOPOLE! (3 kompletne grupy) ");
                persistGameResults(session, p);
                return;
            }
        }

        for (GamePlayer p : active) {
            if (countKurortyOwned(p) >= 4) {
                session.setStatus(GameStatus.FINISHED);
                msg.append(p.getDisplayName()).append(" WYGRYWA przez KURORTY! (wszystkie 4) ");
                persistGameResults(session, p);
                return;
            }
        }

        if (active.size() == 1) {
            session.setStatus(GameStatus.FINISHED);
            GamePlayer winner = active.get(0);
            msg.append(winner.getDisplayName()).append(" WYGRYWA GRE! ");
            persistGameResults(session, winner);
        } else if (active.isEmpty()) {
            session.setStatus(GameStatus.FINISHED);
            msg.append("Wszyscy bankruci — remis. ");
            persistGameResults(session, null);
        } else {

            boolean onlyBots = active.stream().allMatch(p -> p.getUser() == null);
            if (onlyBots) {
                session.setStatus(GameStatus.FINISHED);
                GamePlayer richest = active.stream()
                        .max(java.util.Comparator.comparingInt(GamePlayer::getCash))
                        .orElse(active.get(0));
                msg.append("Wszyscy ludzie opuscili gre — ").append(richest.getDisplayName())
                        .append(" (bot) wygrywa! Gra zakonczona automatycznie.");
                persistGameResults(session, richest);
            }
        }
    }

    private int countColorMonopolies(GamePlayer p) {
        int count = 0;
        for (int[] group : GameEconomy.COLOR_GROUPS) {
            boolean hasAll = true;
            for (int pos : group) {
                if (!p.getOwnedPositions().contains(pos)) { hasAll = false; break; }
            }
            if (hasAll) count++;
        }
        return count;
    }

    private int countKurortyOwned(GamePlayer p) {
        int count = 0;
        for (int i = 0; i < 40; i++) {
            if (GameEconomy.RESORT_TILES[i] && p.getOwnedPositions().contains(i)) count++;
        }
        return count;
    }

    private void appendWinAlerts(List<GamePlayer> players, StringBuilder msg) {
        for (GamePlayer p : players) {
            if (p.isBankrupt()) continue;
            int monopoles = countColorMonopolies(p);
            if (monopoles == 1 || monopoles == 2) {
                int left = 3 - monopoles;
                msg.append("UWAGA! ").append(p.getDisplayName())
                        .append(" jest blisko wygranej przez monopole — zostalo ").append(left)
                        .append(left == 1 ? " grupa! " : " grupy! ");
            }
            int kurorty = countKurortyOwned(p);
            if (kurorty == 2 || kurorty == 3) {
                int left = 4 - kurorty;
                msg.append("UWAGA! ").append(p.getDisplayName())
                        .append(" jest blisko wygranej przez kurorty — zostal").append(left == 1 ? " " : "y ")
                        .append(left).append(left == 1 ? " kurort! " : " kurorty! ");
            }
        }
    }

    private void persistGameResults(GameSession session, GamePlayer winner) {
        long humanPlayers = session.getPlayers().stream().filter(p -> p.getUser() != null).count();
        if (humanPlayers == 0) return;
        int allPlayers = session.getPlayers().size();
        long durationMinutes = ChronoUnit.MINUTES.between(session.getCreatedAt(), LocalDateTime.now());

        List<GamePlayer> ranked = new ArrayList<>(session.getPlayers());
        ranked.sort((a, b) -> {
            boolean aWon = winner != null && winner.getId().equals(a.getId());
            boolean bWon = winner != null && winner.getId().equals(b.getId());
            if (aWon != bWon) return aWon ? -1 : 1;
            if (a.isBankrupt() != b.isBankrupt()) return a.isBankrupt() ? 1 : -1;
            return Integer.compare(b.getCash(), a.getCash());
        });
        Map<Long, Integer> placements = new HashMap<>();
        for (int i = 0; i < ranked.size(); i++) {
            placements.put(ranked.get(i).getId(), i + 1);
        }

        for (GamePlayer gp : session.getPlayers()) {
            if (gp.getUser() == null) continue;
            User user = userRepository.findById(gp.getUser().getId()).orElse(null);
            if (user == null) continue;
            boolean won = winner != null && winner.getId().equals(gp.getId());
            int eloChange = won ? 20 + (allPlayers - 1) * 5 : -10;

            MatchHistory mh = new MatchHistory();
            mh.setUser(user);
            mh.setBoardName("Kampus PB");
            mh.setWon(won);
            mh.setPlacement(placements.getOrDefault(gp.getId(), allPlayers));
            mh.setPlayersCount(allPlayers);
            mh.setFinalCash(gp.getCash());
            mh.setDurationMinutes((int) Math.max(1, durationMinutes));
            mh.setEloChange(eloChange);
            matchHistoryRepository.save(mh);

            int coinReward = won ? GameEconomy.COINS_WIN_REWARD : GameEconomy.COINS_PARTICIPATION;
            user.addCoins(coinReward);

            PlayerStatistics stats = user.getStatistics();
            if (stats != null) {
                stats.setGamesPlayed(stats.getGamesPlayed() + 1);
                if (won) {
                    stats.setGamesWon(stats.getGamesWon() + 1);
                    stats.setWinStreak(stats.getWinStreak() + 1);

                    stats.setAvailableLootboxes(Math.min(stats.getAvailableLootboxes() + 1, 10));
                } else {
                    stats.setWinStreak(0);
                }
                int newElo = Math.max(0, stats.getEloPoints() + eloChange);
                stats.setEloPoints(newElo);
                stats.setLevel(GameEconomy.levelForElo(newElo));
            }

            achievementService.checkAndAward(user.getId());
        }
    }

    private static final Map<String, String> PAWN_COLORS = Map.of(
            "color-blue",    "#38bdf8",
            "color-red",     "#f43f5e",
            "color-emerald", "#10b981",
            "color-pink",    "#ec4899",
            "color-neon",    "#a3e635"
    );

    private String equippedColor(User user, String defaultColor) {
        if (user == null || user.getId() == null) return defaultColor;
        List<OwnedItem> equipped = ownedItemRepository.findByUserIdAndEquipped(user.getId(), true);
        for (OwnedItem item : equipped) {
            String hex = PAWN_COLORS.get(item.getItemSlug());
            if (hex != null) return hex;
        }
        return defaultColor;
    }

    private String uniqueColor(User user, GameSession session) {
        Set<String> taken = session.getPlayers().stream()
                .map(GamePlayer::getColor)
                .collect(Collectors.toSet());
        String preferred = equippedColor(user, null);
        if (preferred != null && !taken.contains(preferred)) return preferred;
        for (String c : COLORS) {
            if (!taken.contains(c)) return c;
        }
        return COLORS[session.getPlayers().size() % COLORS.length];
    }

    private String equippedPawnModel(User user) {
        if (user == null || user.getId() == null) return null;
        return PawnModelCatalog.equippedModelPath(
                ownedItemRepository.findByUserIdAndEquipped(user.getId(), true));
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

        GameStateDto state = buildPublicState(sessionId, d1, d2, message, movedId, fromPos, toPos, card);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    gameSyncService.broadcast(sessionId, state);
                }
            });
        } else {
            gameSyncService.broadcast(sessionId, state);
        }
    }

    public void broadcastReaction(Long sessionId, String username, String reactionCode) {
        GameSession session = getSession(sessionId);
        GamePlayer player = findPlayerByUsername(session, username);

        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "REACTION");
        payload.put("playerId", player.getId());
        payload.put("code", reactionCode);

        if (reactionCode.startsWith("emoji-")) {
            LootboxService.LootboxItem item = LootboxService.findBySlug(reactionCode);
            if (item != null) payload.put("icon", item.iconClass());
        }

        gameSyncService.broadcastReaction(sessionId, payload);
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
        players.get(next).setRollsThisTurn(0);
        players.get(next).setDoublesCount(0);
    }

    private void endTurnOrExtraRoll(GameSession session, GamePlayer player) {
        Long extra = session.getPendingExtraRollPlayerId();
        if (player != null && extra != null && extra.equals(player.getId()) && !player.isBankrupt()) {
            normalizeCurrentTurn(session);
            return;
        }
        session.setPendingExtraRollPlayerId(null);
        advanceTurn(session);
    }

    private void ensureLeader(GameSession session) {
        if (session.getLeaderId() != null) return;
        session.getPlayers().stream()
                .filter(p -> p.getUser() != null)
                .findFirst()
                .ifPresent(p -> session.setLeaderId(p.getUser().getId()));
    }

    private void beginActivePlay(GameSession session) {
        session.clearPendingPurchase();
        session.clearPendingPayment();
        session.clearPendingUpgrade();
        session.clearPendingBuyback();
        session.setPendingExtraRollPlayerId(null);
        List<GamePlayer> players = session.getPlayers();
        for (int i = 0; i < players.size(); i++) {
            GamePlayer p = players.get(i);
            if (p.getUser() != null && !p.isBankrupt()) {
                session.setCurrentTurn(i);
                return;
            }
        }
        normalizeCurrentTurn(session);
    }

    private void normalizeCurrentTurn(GameSession session) {
        List<GamePlayer> players = session.getPlayers();
        int n = players.size();
        if (n == 0) return;
        int start = session.getCurrentTurn() % n;
        for (int i = 0; i < n; i++) {
            int idx = (start + i) % n;
            if (!players.get(idx).isBankrupt()) {
                session.setCurrentTurn(idx);
                return;
            }
        }
    }

    private int resolvePurchasePrice(GameSession session, int pos) {
        if (session.getPendingPurchasePrice() != null) {
            return session.getPendingPurchasePrice();
        }
        if (pos >= 0 && pos < TILE_PRICE.length) {
            return TILE_PRICE[pos];
        }
        return 0;
    }

    private PendingPurchaseDto buildPendingPurchase(GameSession session) {
        Integer posObj = session.getPendingPurchasePos();
        if (posObj == null) return null;
        int pos = posObj;
        if (pos < 0 || pos >= TILES.length) return null;
        int price = resolvePurchasePrice(session, pos);
        int baseRent = GameEconomy.rentForLevel(pos, 0, false);
        return new PendingPurchaseDto(
                pos,
                TILES[pos],
                price,
                session.getPendingDeciderId(),
                Math.max(50, price / 2),
                baseRent
        );
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

        Long effectiveLeaderId = session.getLeaderId();
        if (effectiveLeaderId == null && session.getStatus() == GameStatus.WAITING) {
            effectiveLeaderId = players.stream()
                    .filter(p -> p.getUser() != null)
                    .map(p -> p.getUser().getId())
                    .findFirst().orElse(null);
        }

        List<GamePlayerDto> dtos = new ArrayList<>();
        for (GamePlayer p : players) {
            boolean isMe = username != null && p.getUser() != null
                    && p.getUser().getUsername().equals(username);
            boolean isBot = p.getUser() == null;
            boolean isLeader = p.getUser() != null && effectiveLeaderId != null
                    && effectiveLeaderId.equals(p.getUser().getId());
            String pawnModel = equippedPawnModel(p.getUser());
            dtos.add(new GamePlayerDto(p.getId(), p.getDisplayName(), p.getCash(),
                    p.getPosition(), p.getColor(), p.isBankrupt(), isMe, isBot,
                    new ArrayList<>(p.getOwnedPositions()),
                    new HashMap<>(p.getPropertyLevels()),
                    p.isReady(), isLeader, pawnModel));
        }
        Long currentId = players.isEmpty() ? null
                : players.get(session.getCurrentTurn() % players.size()).getId();
        boolean myTurn = username != null && currentId != null && dtos.stream()
                .anyMatch(d -> d.isMe() && d.id().equals(currentId));
        boolean canRollAgain = currentId != null
                && session.getPendingExtraRollPlayerId() != null
                && session.getPendingExtraRollPlayerId().equals(currentId);

        PendingPurchaseDto pending = buildPendingPurchase(session);

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
            } else if (!active.isEmpty()) {

                GamePlayer monopoleWinner = active.stream()
                        .filter(p -> countColorMonopolies(p) >= 3)
                        .findFirst().orElse(null);
                if (monopoleWinner != null) {
                    winnerId = monopoleWinner.getId();
                    winnerName = monopoleWinner.getDisplayName();
                } else {
                    GamePlayer kurortWinner = active.stream()
                            .filter(p -> countKurortyOwned(p) >= 4)
                            .findFirst().orElse(null);
                    if (kurortWinner != null) {
                        winnerId = kurortWinner.getId();
                        winnerName = kurortWinner.getDisplayName();
                    } else {

                        GamePlayer richest = active.stream()
                                .max(java.util.Comparator.comparingInt(GameEconomy::netWorth))
                                .orElse(active.get(0));
                        winnerId = richest.getId();
                        winnerName = richest.getDisplayName();
                    }
                }
            }
        }

        List<Integer> prices = new ArrayList<>();
        for (int v : TILE_PRICE) prices.add(v);

        List<HandCardDto> myHandCards = null;
        if (username != null) {
            for (GamePlayer p : players) {
                if (p.getUser() != null && p.getUser().getUsername().equals(username)) {
                    myHandCards = p.getHandCards().stream().map(name -> {
                        try {
                            HandCardType t = HandCardType.valueOf(name);
                            return new HandCardDto(name, t.label, t.description, t.iconClass);
                        } catch (IllegalArgumentException ex) {
                            return null;
                        }
                    }).filter(java.util.Objects::nonNull).toList();
                    break;
                }
            }
        }

        PendingUpgradeDto pendingUpgrade = null;
        if (session.getPendingUpgradePos() != null) {
            int uPos = session.getPendingUpgradePos();
            int curLevel = 0;
            GamePlayer upgradeOwner = null;
            if (session.getPendingUpgradePlayerId() != null) {
                upgradeOwner = players.stream()
                        .filter(p -> p.getId().equals(session.getPendingUpgradePlayerId()))
                        .findFirst().orElse(null);
                if (upgradeOwner != null) curLevel = upgradeOwner.getPropertyLevels().getOrDefault(uPos, 0);
            }
            pendingUpgrade = new PendingUpgradeDto(
                    uPos, TILES[uPos],
                    session.getPendingUpgradeCost() != null ? session.getPendingUpgradeCost() : upgradeCost(uPos),
                    curLevel, computeRentForLevel(uPos, curLevel + 1, upgradeOwner),
                    session.getPendingUpgradePlayerId());
        }

        PendingBuybackDto pendingBuyback = null;
        if (session.getPendingBuybackPos() != null && session.getPendingBuybackPrice() != null) {
            int bPos = session.getPendingBuybackPos();
            GamePlayer victim = players.stream()
                    .filter(p -> p.getId().equals(session.getPendingBuybackVictimId()))
                    .findFirst().orElse(null);
            GamePlayer holder = players.stream()
                    .filter(p -> p.getId().equals(session.getPendingBuybackHolderId()))
                    .findFirst().orElse(null);
            pendingBuyback = new PendingBuybackDto(
                    bPos,
                    TILES[bPos],
                    session.getPendingBuybackPrice(),
                    session.getPendingBuybackVictimId(),
                    session.getPendingBuybackHolderId(),
                    holder != null ? holder.getDisplayName() : "?",
                    victim != null ? sellPricesFor(victim) : Map.of()
            );
        }

        PendingTakeoverDto pendingTakeover = null;
        if (session.getPendingTakeoverPos() != null && session.getPendingTakeoverPrice() != null) {
            int tPos = session.getPendingTakeoverPos();
            GamePlayer seller = players.stream()
                    .filter(p -> p.getId().equals(session.getPendingTakeoverSellerId()))
                    .findFirst().orElse(null);
            pendingTakeover = new PendingTakeoverDto(
                    tPos,
                    TILES[tPos],
                    session.getPendingTakeoverPrice(),
                    session.getPendingTakeoverBuyerId(),
                    session.getPendingTakeoverSellerId(),
                    seller != null ? seller.getDisplayName() : "?");
        }

        List<PropertyCardDto> myPropertyCards = buildMyPropertyCards(session, username);

        Long secondsLeft = null;
        if (session.getStatus() == GameStatus.ACTIVE && session.getCreatedAt() != null) {
            long elapsed = ChronoUnit.SECONDS.between(session.getCreatedAt(), LocalDateTime.now());
            secondsLeft = Math.max(0, GameEconomy.GAME_DURATION_SECONDS - elapsed);
        }

        return new GameStateDto(session.getId(), session.getCode(), session.getName(),
                session.getStatus().name(), dtos, currentId, d1, d2, message,
                movedId, fromPos, toPos, myTurn, tileNamesList(), tileEffectsList(),
                pending, pendingPayment, ownership, prices, card, winnerId, winnerName,
                myHandCards, pendingUpgrade, pendingBuyback, effectiveLeaderId, canRollAgain,
                myPropertyCards, secondsLeft, pendingTakeover);
    }

    private List<PropertyCardDto> buildMyPropertyCards(GameSession session, String username) {
        if (username == null) {
            return null;
        }
        GamePlayer me = session.getPlayers().stream()
                .filter(p -> p.getUser() != null && p.getUser().getUsername().equals(username))
                .findFirst().orElse(null);
        if (me == null || me.isBankrupt()) {
            return List.of();
        }
        List<PropertyCardDto> cards = new ArrayList<>();
        for (int pos : me.getOwnedPositions().stream().sorted().toList()) {
            int buy = TILE_PRICE[pos];
            if (buy <= 0) {
                continue;
            }
            int level = me.getPropertyLevels().getOrDefault(pos, 0);
            int rent = computeRentForLevel(pos, level, me);
            int upCost = GameEconomy.upgradeCost(pos);
            int nextRent = level < GameEconomy.MAX_PROPERTY_LEVEL
                    ? computeRentForLevel(pos, level + 1, me) : rent;
            cards.add(new PropertyCardDto(
                    pos,
                    TILES[pos],
                    buy,
                    rent,
                    sellPrice(pos),
                    upCost,
                    nextRent,
                    level,
                    propertyLevelLabel(level),
                    level < GameEconomy.MAX_PROPERTY_LEVEL && upCost > 0
            ));
        }
        return cards;
    }

    @Transactional
    public GameStateDto buybackProperty(Long sessionId, String username) {
        return withLock(sessionId, () -> buybackPropertyInternal(sessionId, username));
    }

    private GameStateDto buybackPropertyInternal(Long sessionId, String username) {
        GameSession session = getSession(sessionId);
        requireActiveGame(session);
        if (session.getPendingBuybackVictimId() == null) {
            throw new IllegalArgumentException("Brak aktywnego odkupu.");
        }
        GamePlayer me = findPlayerByUsername(session, username);
        if (!me.getId().equals(session.getPendingBuybackVictimId())) {
            throw new IllegalArgumentException("To nie Twoje pole do odkupu.");
        }
        int price = session.getPendingBuybackPrice();
        int pos = session.getPendingBuybackPos();
        if (me.getCash() < price) {
            throw new IllegalArgumentException("Za malo siana na odkup (potrzeba " + price
                    + " PLN). Sprzedaj inna nieruchomosc z panelu po lewej.");
        }
        GamePlayer holder = session.getPlayers().stream()
                .filter(p -> p.getId().equals(session.getPendingBuybackHolderId()))
                .findFirst().orElseThrow();
        me.setCash(me.getCash() - price);
        holder.setCash(holder.getCash() + price);
        holder.getOwnedPositions().remove(pos);
        clearPropertyDevelopment(holder, pos);
        me.getOwnedPositions().add(pos);
        clearPropertyDevelopment(me, pos);
        session.clearPendingBuyback();
        persist(session);

        String msg = me.getDisplayName() + " odkupuje " + TILES[pos] + " za " + price + " PLN od "
                + holder.getDisplayName() + "!";
        publishPublic(sessionId, null, null, msg, null, null, null, null);
        scheduleBotUpdate(sessionId);
        return toState(session, username, null, null, msg, null, null, null, null);
    }

    @Transactional
    public GameStateDto skipBuyback(Long sessionId, String username) {
        return withLock(sessionId, () -> skipBuybackInternal(sessionId, username));
    }

    private GameStateDto skipBuybackInternal(Long sessionId, String username) {
        GameSession session = getSession(sessionId);
        requireActiveGame(session);
        if (session.getPendingBuybackVictimId() == null) {
            throw new IllegalArgumentException("Brak aktywnego odkupu.");
        }
        GamePlayer me = findPlayerByUsername(session, username);
        if (!me.getId().equals(session.getPendingBuybackVictimId())) {
            throw new IllegalArgumentException("To nie Twoja decyzja o odkupie.");
        }
        Integer posObj = session.getPendingBuybackPos();
        GamePlayer holder = session.getPlayers().stream()
                .filter(p -> p.getId().equals(session.getPendingBuybackHolderId()))
                .findFirst().orElse(null);
        session.clearPendingBuyback();
        persist(session);

        String msg = me.getDisplayName() + " rezygnuje z odkupu "
                + (posObj != null ? TILES[posObj] : "pola")
                + (holder != null ? " — zostaje u " + holder.getDisplayName() + "." : ".");
        publishPublic(sessionId, null, null, msg, null, null, null, null);
        scheduleBotUpdate(sessionId);
        return toState(session, username, null, null, msg, null, null, null, null);
    }

    @Transactional
    public GameStateDto autoBuybackTimeout(Long sessionId, int snapPos, Long snapVictimId) {
        return withLock(sessionId, () -> autoBuybackTimeoutInternal(sessionId, snapPos, snapVictimId));
    }

    private GameStateDto autoBuybackTimeoutInternal(Long sessionId, int snapPos, Long snapVictimId) {
        GameSession session = getSession(sessionId);
        if (session.getPendingBuybackVictimId() == null) {
            scheduleBotUpdate(sessionId);
            return null;
        }
        if (session.getPendingBuybackPos() == null || session.getPendingBuybackPos() != snapPos) {
            scheduleBotUpdate(sessionId);
            return null;
        }
        if (!snapVictimId.equals(session.getPendingBuybackVictimId())) {
            scheduleBotUpdate(sessionId);
            return null;
        }
        GamePlayer victim = session.getPlayers().stream()
                .filter(p -> p.getId().equals(snapVictimId))
                .findFirst().orElse(null);
        if (victim == null) {
            scheduleBotUpdate(sessionId);
            return null;
        }
        int price = session.getPendingBuybackPrice() != null ? session.getPendingBuybackPrice() : 0;
        String username = victim.getUser() != null ? victim.getUser().getUsername() : null;
        if (victim.getCash() >= price) {
            return buybackProperty(sessionId, username);
        }
        return skipBuyback(sessionId, username);
    }

    @Transactional
    public GameStateDto resolveBuybackAsBot(Long sessionId, Long victimId, int snapPos, int snapPrice) {
        return withLock(sessionId, () -> resolveBuybackAsBotInternal(sessionId, victimId, snapPos, snapPrice));
    }

    private GameStateDto resolveBuybackAsBotInternal(Long sessionId, Long victimId, int snapPos, int snapPrice) {
        GameSession session = getSession(sessionId);
        if (session.getPendingBuybackVictimId() == null) {
            scheduleBotUpdate(sessionId);
            return null;
        }
        if (!victimId.equals(session.getPendingBuybackVictimId())) {
            scheduleBotUpdate(sessionId);
            return null;
        }
        if (session.getPendingBuybackPos() == null || session.getPendingBuybackPos() != snapPos) {
            scheduleBotUpdate(sessionId);
            return null;
        }
        if (session.getPendingBuybackPrice() == null || session.getPendingBuybackPrice() != snapPrice) {
            scheduleBotUpdate(sessionId);
            return null;
        }
        GamePlayer victim = session.getPlayers().stream()
                .filter(p -> p.getId().equals(victimId))
                .findFirst().orElse(null);
        if (victim == null || victim.getUser() != null) {
            scheduleBotUpdate(sessionId);
            return null;
        }
        while (victim.getCash() < snapPrice && !victim.getOwnedPositions().isEmpty()) {
            int pos = session.getPendingBuybackPos();
            int cheapest = victim.getOwnedPositions().stream()
                    .filter(p -> p != pos)
                    .min((a, b) -> Integer.compare(sellPrice(a), sellPrice(b)))
                    .orElse(-1);
            if (cheapest < 0) break;
            doSellProperty(session, victim, cheapest);
        }
        if (victim.getCash() >= snapPrice) {
            return buybackProperty(sessionId, null);
        }
        return skipBuyback(sessionId, null);
    }

    @Transactional
    public GameStateDto playCard(Long sessionId, Long botPlayerId, String cardType) {
        return withLock(sessionId, () -> playCardBotInternal(sessionId, botPlayerId, cardType));
    }

    private GameStateDto playCardBotInternal(Long sessionId, Long botPlayerId, String cardType) {
        GameSession session = getSession(sessionId);
        requireActiveGame(session);
        GamePlayer me = session.getPlayers().stream()
                .filter(p -> p.getId().equals(botPlayerId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Bot nie znaleziony: " + botPlayerId));
        if (!me.getHandCards().contains(cardType)) return null;
        HandCardType type;
        try {
            type = HandCardType.valueOf(cardType);
        } catch (IllegalArgumentException ex) {
            return null;
        }

        if (type == HandCardType.TELEPORT || type == HandCardType.FREE_UPGRADE
                || type == HandCardType.DESTROY_PROPERTY) {
            return null;
        }
        me.getHandCards().remove(cardType);
        StringBuilder msg = new StringBuilder();
        msg.append(me.getDisplayName()).append(" (bot) zagrywa: ").append(type.label).append(". ");
        switch (type) {
            case SKIP_RENT -> me.setSkipNextRent(true);
            case EXTRA_ROLL -> session.setPendingExtraRollPlayerId(me.getId());
            case ADD_CASH -> {
                me.setCash(me.getCash() + GameEconomy.GO_BONUS);
                msg.append("+").append(GameEconomy.GO_BONUS).append(" PLN. ");
            }
            case SHIELD -> me.setShieldActive(true);
            case JAIL_PASS -> {
                if (me.getPosition() == POS_JAIL) {
                    me.setPosition(POS_JAIL + 1);
                } else {
                    me.setJailPassActive(true);
                }
            }
            case DOUBLE_RENT_NEXT -> me.setDoubleRentNext(true);
            case SCHOLARSHIP_ALL -> {
                int earned = 0;
                for (GamePlayer other : session.getPlayers()) {
                    if (other.getId().equals(me.getId()) || other.isBankrupt()) continue;
                    int pay = Math.min(other.getCash(), 100_000);
                    other.setCash(other.getCash() - pay);
                    earned += pay;
                }
                me.setCash(me.getCash() + earned);
                msg.append("Inkasuje ").append(earned).append(" PLN. ");
            }
            default -> {}
        }
        persist(session);
        publishPublic(session.getId(), null, null, msg.toString(), null, null, null, null);
        scheduleBotUpdate(session.getId());
        return buildPublicState(session.getId(), null, null, msg.toString(), null, null, null, null);
    }

    @Transactional
    public GameStateDto playCard(Long sessionId, String username, String cardType, Integer targetPos) {
        return withLock(sessionId, () -> playCardInternal(sessionId, username, cardType, targetPos));
    }

    private GameStateDto playCardInternal(Long sessionId, String username, String cardType, Integer targetPos) {
        GameSession session = getSession(sessionId);
        requireParticipant(session, username);
        requireActiveGame(session);

        GamePlayer me = findPlayerByUsername(session, username);
        if (!me.getHandCards().contains(cardType)) {
            throw new IllegalArgumentException("Nie masz tej karty: " + cardType);
        }

        HandCardType type;
        try {
            type = HandCardType.valueOf(cardType);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Nieznany typ karty: " + cardType);
        }
        StringBuilder msg = new StringBuilder();
        msg.append(me.getDisplayName()).append(" zagrywa kartę: ").append(type.label).append(". ");

        switch (type) {
            case SKIP_RENT -> {
                me.setSkipNextRent(true);
                msg.append("Nastepny czynsz zostanie pominiety. ");
            }
            case EXTRA_ROLL -> {
                session.setPendingExtraRollPlayerId(me.getId());
                msg.append("Gracz dostaje dodatkowy rzut po tej turze. ");
            }
            case ADD_CASH -> {
                me.setCash(me.getCash() + GameEconomy.GO_BONUS);
                msg.append("+").append(GameEconomy.GO_BONUS).append(" PLN od banku! ");
            }
            case SHIELD -> {
                me.setShieldActive(true);
                msg.append("Tarcza Akademicka aktywna — absorbuje nastepna oplate do ")
                        .append(GameEconomy.SHIELD_MAX_ABSORB).append(" PLN. ");
            }
            case DESTROY_PROPERTY -> {
                if (targetPos == null) {
                    throw new IllegalArgumentException("Podaj pozycje pola do przejecia.");
                }
                if (RESORT_TILES[targetPos] || UTILITY_TILES[targetPos] || CHANCE_TILES[targetPos]) {
                    throw new IllegalArgumentException("Te pole nie podlega przejeciu.");
                }
                GamePlayer owner = findOwner(session, targetPos);
                if (owner == null) {
                    throw new IllegalArgumentException("To pole nie ma wlasciciela.");
                }
                if (owner.getId().equals(me.getId())) {
                    throw new IllegalArgumentException("Nie mozesz przejac wlasnego pola.");
                }
                owner.getOwnedPositions().remove(targetPos);
                clearPropertyDevelopment(owner, targetPos);
                me.getOwnedPositions().add(targetPos);
                clearPropertyDevelopment(me, targetPos);

                int buybackPrice = TILE_PRICE[targetPos] * 2;
                session.setPendingBuybackPos(targetPos);
                session.setPendingBuybackVictimId(owner.getId());
                session.setPendingBuybackHolderId(me.getId());
                session.setPendingBuybackPrice(buybackPrice);

                msg.append("Przejmuje pole ").append(TILES[targetPos])
                        .append(" od ").append(owner.getDisplayName()).append("! ")
                        .append(owner.getDisplayName())
                        .append(" moze odkupic za ").append(buybackPrice).append(" PLN (2x cena). ");
            }
            case FREE_UPGRADE -> {
                if (targetPos == null) {
                    throw new IllegalArgumentException("Wybierz pole do darmowego ulepszenia.");
                }
                if (!me.getOwnedPositions().contains(targetPos)) {
                    throw new IllegalArgumentException("Nie posiadasz tego pola.");
                }
                if (TILE_PRICE[targetPos] <= 0 || RESORT_TILES[targetPos] || UTILITY_TILES[targetPos]) {
                    throw new IllegalArgumentException("To pole nie podlega ulepszeniu.");
                }
                if (!GameEconomy.hasColorMonopoly(me, targetPos)) {
                    throw new IllegalArgumentException("Potrzebujesz monopolu kolorystycznego (caly kolor).");
                }
                int currLvl = me.getPropertyLevels().getOrDefault(targetPos, 0);
                if (currLvl >= GameEconomy.MAX_PROPERTY_LEVEL) {
                    throw new IllegalArgumentException("Pole jest juz na maksymalnym poziomie (" + GameEconomy.MAX_PROPERTY_LEVEL + ").");
                }
                me.getPropertyLevels().put(targetPos, currLvl + 1);
                String lvlLabel = upgradeBuiltLabel(currLvl + 1);
                msg.append("Akademickie Pozwolenie Budowlane! ")
                        .append(me.getDisplayName()).append(" buduje ").append(lvlLabel)
                        .append(" na ").append(TILES[targetPos]).append(" za darmo! ");
            }
            case JAIL_PASS -> {
                if (me.getPosition() == POS_JAIL) {
                    me.setPosition(POS_JAIL + 1);
                    msg.append("Przepustka z Dziekanatu! ").append(me.getDisplayName())
                            .append(" omija oplate i wychodzi na pole ").append(TILES[POS_JAIL + 1]).append(". ");
                } else {
                    me.setJailPassActive(true);
                    msg.append("Przepustka z Dziekanatu aktywna — nastepna wizyta w Dziekanacie bezplatna. ");
                }
            }
            case DOUBLE_RENT_NEXT -> {
                me.setDoubleRentNext(true);
                msg.append("Podwojny Wymagacz aktywny — nastepny czynsz zebrany od rywala wyniesie 2x. ");
            }
            case TELEPORT -> {
                if (targetPos == null || targetPos < 0 || targetPos >= 40) {
                    throw new IllegalArgumentException("Podaj prawidlowa pozycje pola (0-39).");
                }
                me.setPosition(targetPos);
                msg.append("Teleport Kampusowy! ").append(me.getDisplayName())
                        .append(" teleportuje sie na pole ").append(TILES[targetPos]).append(". ");
            }
            case SCHOLARSHIP_ALL -> {
                int totalEarned = 0;
                for (GamePlayer other : session.getPlayers()) {
                    if (other.getId().equals(me.getId()) || other.isBankrupt()) continue;
                    int pay = Math.min(other.getCash(), 100_000);
                    other.setCash(other.getCash() - pay);
                    totalEarned += pay;
                }
                me.setCash(me.getCash() + totalEarned);
                msg.append("Zbiorowe Stypendium! ").append(me.getDisplayName())
                        .append(" inkasuje ").append(totalEarned).append(" PLN od wszystkich graczy. ");
            }
        }

        me.getHandCards().remove(cardType);
        persist(session);

        publishPublic(sessionId, null, null, msg.toString(), null, null, null, null);
        scheduleBotUpdate(sessionId);
        return toState(session, username, null, null, msg.toString(), null, null, null, null);
    }

    @Transactional
    public GameStateDto upgrade(Long sessionId, String username) {
        return withLock(sessionId, () -> upgradeInternal(sessionId, username));
    }

    private GameStateDto upgradeInternal(Long sessionId, String username) {
        GameSession session = getSession(sessionId);
        requireParticipant(session, username);
        requireActiveGame(session);

        if (session.getPendingUpgradePos() == null) {
            throw new IllegalArgumentException("Brak aktywnego ulepszenia.");
        }
        GamePlayer me = findPlayerByUsername(session, username);
        if (!me.getId().equals(session.getPendingUpgradePlayerId())) {
            throw new IllegalArgumentException("To nie Twoje pole do ulepszenia.");
        }

        int cost = session.getPendingUpgradeCost();
        if (me.getCash() < cost) {
            session.setPendingPaymentDebtorId(me.getId());
            session.setPendingPaymentAmount(cost);
            session.setPendingPaymentCreditorId(null);
            session.setPendingPaymentReason("Ulepszenie: " + TILES[session.getPendingUpgradePos()]);
            persist(session);
            String msg = me.getDisplayName() + " — za malo siana na ulepszenie (" + cost
                    + " PLN). Sprzedaj nieruchomosc z panelu po lewej.";
            publishPublic(sessionId, null, null, msg, null, null, null, null);
            scheduleBotUpdate(sessionId);
            return toState(session, username, null, null, msg, null, null, null, null);
        }
        return doUpgrade(session, me, username);
    }

    private GameStateDto doUpgrade(GameSession session, GamePlayer me, String username) {
        int pos = session.getPendingUpgradePos();
        int cost = session.getPendingUpgradeCost();
        int currentLevel = me.getPropertyLevels().getOrDefault(pos, 0);
        int newLevel = currentLevel + 1;
        me.setCash(me.getCash() - cost);
        me.getPropertyLevels().put(pos, newLevel);
        session.clearPendingUpgrade();
        endTurnOrExtraRoll(session, me);
        persist(session);

        String levelName = upgradeBuiltLabel(newLevel);
        int newRent = computeRentForLevel(pos, newLevel, me);
        String msg = me.getDisplayName() + " buduje " + levelName + " na " + TILES[pos]
                + " za " + cost + " PLN! Nowy czynsz: " + newRent + " PLN.";
        Long sessionId = session.getId();
        publishPublic(sessionId, null, null, msg, null, null, null, null);
        scheduleBotUpdate(sessionId);
        return toState(session, username, null, null, msg, null, null, null, null);
    }

    @Transactional
    public GameStateDto autoUpgradeTimeout(Long sessionId, int snapPos, Long snapPlayerId) {
        return withLock(sessionId, () -> autoUpgradeTimeoutInternal(sessionId, snapPos, snapPlayerId));
    }

    private GameStateDto autoUpgradeTimeoutInternal(Long sessionId, int snapPos, Long snapPlayerId) {
        GameSession session = getSession(sessionId);
        if (session.getPendingUpgradePos() == null) {
            scheduleBotUpdate(sessionId);
            return null;
        }
        if (session.getPendingUpgradePos() != snapPos) {
            scheduleBotUpdate(sessionId);
            return null;
        }
        Long deciderId = session.getPendingUpgradePlayerId();
        if (deciderId == null || !deciderId.equals(snapPlayerId)) {
            scheduleBotUpdate(sessionId);
            return null;
        }
        GamePlayer decider = session.getPlayers().stream()
                .filter(p -> p.getId().equals(deciderId))
                .findFirst().orElse(null);
        if (decider == null) {
            scheduleBotUpdate(sessionId);
            return null;
        }
        String username = decider.getUser() != null ? decider.getUser().getUsername() : null;
        return doSkipUpgrade(session, decider, username, true);
    }

    @Transactional
    public GameStateDto skipUpgrade(Long sessionId, String username) {
        return withLock(sessionId, () -> skipUpgradeInternal(sessionId, username));
    }

    private GameStateDto skipUpgradeInternal(Long sessionId, String username) {
        GameSession session = getSession(sessionId);
        requireParticipant(session, username);
        if (session.getPendingUpgradePos() == null) {
            throw new IllegalArgumentException("Brak aktywnego ulepszenia.");
        }
        GamePlayer me = findPlayerByUsername(session, username);
        if (!me.getId().equals(session.getPendingUpgradePlayerId())) {
            throw new IllegalArgumentException("To nie Twoje pole.");
        }
        return doSkipUpgrade(session, me, username, false);
    }

    private GameStateDto doSkipUpgrade(GameSession session, GamePlayer me, String username, boolean timeout) {
        session.clearPendingUpgrade();
        endTurnOrExtraRoll(session, me);
        checkGameEnd(session, new StringBuilder());
        persist(session);

        String msg = timeout
                ? me.getDisplayName() + " pomija ulepszenie (czas minal)."
                : me.getDisplayName() + " pomija ulepszenie pola.";
        Long sessionId = session.getId();
        publishPublic(sessionId, null, null, msg, null, null, null, null);
        scheduleBotUpdate(sessionId);
        return toState(session, username, null, null, msg, null, null, null, null);
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

    public record ChanceCard(String title, String description, int moneyEffect) {}
}
