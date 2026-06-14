package pl.pb.monopoly.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.pb.monopoly.domain.GamePlayer;
import pl.pb.monopoly.domain.GameSession;
import pl.pb.monopoly.repository.GameSessionRepository;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Automatyzacja: <ul>
 *   <li>boty rzucaja kostka 1.5s po przejsciu na ich ture,</li>
 *   <li>boty decyduja pendingPurchase (kupuja gdy cash &gt; 2x cena, inaczej skipuja) po 1.3s,</li>
 *   <li>boty decyduja pendingUpgrade po 1.3s (nie czekaja 15s jak czlowiek),</li>
 *   <li>czlowiek ma 25s na decyzje pendingPurchase - po tym czasie auto-skip.</li>
 * </ul>
 *
 * Wszystkie zaplanowane akcje wywoluja {@link GameService} ktore zapisuje stan i broadcastuje
 * przez WebSocket. {@link BotAutoplayService#onTurnUpdate(Long)} jest hookiem wolanym po kazdej
 * zmianie stanu by lancuchowo planowac kolejne akcje.
 */
@Service
public class BotAutoplayService {

    private static final Logger log = LoggerFactory.getLogger(BotAutoplayService.class);

    /** Opoznienie miedzy zakonczeniem stanu a rzutem bota - dramaturgia. */
    private static final long BOT_ROLL_DELAY_MS = 1500;
    /** Bot decyduje (kupno/ulepszenie/splata) dopiero po animacji ruchu u klienta (~5s). */
    private static final long BOT_DECISION_DELAY_MS = 4200;
    /** Czas na decyzje gracza-czlowieka (kupno pola). Liczony od rzutu, a animacja
        pionka u klienta trwa kilka sekund — stad zapas, by zdazyc kliknac Kupuj/Pomin. */
    private static final long HUMAN_DECISION_TIMEOUT_SECONDS = 25;
    /** Czas na splate zadluzenia (sprzedaz, pozyczka). */
    private static final long HUMAN_PAYMENT_TIMEOUT_SECONDS = 30;
    /** Czas na decyzje czlowieka o ulepszeniu pola. */
    private static final long HUMAN_UPGRADE_TIMEOUT_SECONDS = 15;
    /** Krotki retry gdy akcja bota zwrocila null lub rzucila wyjatek. */
    private static final long BOT_RECOVERY_DELAY_MS = 600;

    /** Sesja jest "aktywna dla watchdoga" tylko przez ten czas od ostatniej akcji. */
    private static final long WATCHDOG_ACTIVE_WINDOW_MS = 90_000;

    private final GameSessionRepository sessionRepository;
    private final GameService gameService;
    /** Self-proxy — by watchdog/recovery wolaly onTurnUpdate przez proxy (z @Transactional). */
    private final ObjectProvider<BotAutoplayService> selfProvider;
    private final ScheduledExecutorService scheduler;
    /** Anuluj poprzedni timer sesji — bez tego stare auto-skipy kasuja aktywne kupno. */
    private final ConcurrentHashMap<Long, ScheduledFuture<?>> pendingTasks = new ConcurrentHashMap<>();
    /** Watchdog: ile kolejnych cykli sesja jest bez zaplanowanej akcji (do wykrycia zawieszenia). */
    private final ConcurrentHashMap<Long, Integer> idleCycles = new ConcurrentHashMap<>();
    /** Ostatnia aktywnosc sesji (ms) — watchdog ignoruje porzucone/stare sesje. */
    private final ConcurrentHashMap<Long, Long> lastActivity = new ConcurrentHashMap<>();

    public BotAutoplayService(GameSessionRepository sessionRepository,
                              @Lazy GameService gameService,
                              ObjectProvider<BotAutoplayService> selfProvider) {
        this.sessionRepository = sessionRepository;
        this.gameService = gameService;
        this.selfProvider = selfProvider;
        this.scheduler = Executors.newScheduledThreadPool(2, r -> {
            Thread t = new Thread(r, "bot-autoplay");
            t.setDaemon(true);
            return t;
        });
        // Watchdog: co 5s odblokowuje sesje, w ktorych lancuch botow sie urwal
        // (np. zgubiony broadcast, wyjatek bez retry, niespojny stan po stronie klienta).
        this.scheduler.scheduleWithFixedDelay(this::watchdog, 8, 5, TimeUnit.SECONDS);
    }

    /**
     * Co kilka sekund sprawdza aktywne sesje. Jesli sesja od &gt;=2 cykli (~10s) nie ma
     * zaplanowanej zadnej akcji bota, ponownie wywoluje onTurnUpdate — dzieki temu gra
     * nigdy nie zawisa na turze bota, nawet gdy lancuch zdarzen sie urwal.
     */
    private void watchdog() {
        try {
            long now = System.currentTimeMillis();
            // sprzataj stare wpisy
            lastActivity.entrySet().removeIf(e -> now - e.getValue() > WATCHDOG_ACTIVE_WINDOW_MS);
            for (Long id : new java.util.ArrayList<>(lastActivity.keySet())) {
                if (pendingTasks.containsKey(id)) {
                    idleCycles.remove(id);      // cos jest zaplanowane — sesja zyje
                    continue;
                }
                int idle = idleCycles.merge(id, 1, Integer::sum);
                if (idle >= 2) {
                    idleCycles.remove(id);
                    log.info("Watchdog: odblokowuje sesje {} (brak akcji bota)", id);
                    // przez proxy — inaczej @Transactional sie nie zalaczy (self-invocation)
                    selfProvider.getObject().onTurnUpdate(id);
                }
            }
            idleCycles.keySet().retainAll(lastActivity.keySet());
        } catch (Exception ex) {
            log.warn("Watchdog failed: {}", ex.getMessage());
        }
    }

    /**
     * Wolane po kazdej operacji ktora zmienia stan rozgrywki (roll/buy/skip/bid).
     * Decyduje czy zaplanowac auto-rzut bota lub timeout decyzji czlowieka.
     * Transakcja read-only zapewnia ze lazy associacje (players) sa dostepne.
     */
    @Transactional(readOnly = true)
    public void onTurnUpdate(Long sessionId) {
        if (sessionId == null) return;
        try {
            GameSession s = sessionRepository.findById(sessionId).orElse(null);
            if (s == null) return;
            if (s.getPlayers().isEmpty()) return;
            if (s.getStatus() == pl.pb.monopoly.domain.GameStatus.FINISHED) { clearTracking(sessionId); return; }
            if (s.getStatus() == pl.pb.monopoly.domain.GameStatus.WAITING) { clearTracking(sessionId); return; }
            lastActivity.put(sessionId, System.currentTimeMillis());

            // 1) Aktywna splata zadluzenia
            if (s.getPendingPaymentDebtorId() != null && s.getPendingPaymentAmount() != null) {
                Long debtorId = s.getPendingPaymentDebtorId();
                int snapAmount = s.getPendingPaymentAmount();
                GamePlayer debtor = s.getPlayers().stream()
                        .filter(p -> p.getId().equals(debtorId))
                        .findFirst().orElse(null);
                if (debtor == null) return;
                if (debtor.getUser() == null) {
                    scheduleOnce(sessionId,
                            () -> gameService.resolvePaymentAsBot(sessionId, debtorId, snapAmount),
                            BOT_DECISION_DELAY_MS, TimeUnit.MILLISECONDS);
                } else {
                    scheduleOnce(sessionId,
                            () -> gameService.autoPaymentTimeout(sessionId, snapAmount),
                            HUMAN_PAYMENT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                }
                return;
            }

            // 1b) Odkup posesji po karcie przejecia
            if (s.getPendingBuybackVictimId() != null && s.getPendingBuybackPos() != null
                    && s.getPendingBuybackPrice() != null) {
                int snapPos = s.getPendingBuybackPos();
                Long snapVictim = s.getPendingBuybackVictimId();
                int snapPrice = s.getPendingBuybackPrice();
                GamePlayer victim = s.getPlayers().stream()
                        .filter(p -> p.getId().equals(snapVictim))
                        .findFirst().orElse(null);
                if (victim == null) return;
                if (victim.getUser() == null) {
                    scheduleOnce(sessionId,
                            () -> gameService.resolveBuybackAsBot(sessionId, snapVictim, snapPos, snapPrice),
                            BOT_DECISION_DELAY_MS, TimeUnit.MILLISECONDS);
                } else {
                    scheduleOnce(sessionId,
                            () -> gameService.autoBuybackTimeout(sessionId, snapPos, snapVictim),
                            HUMAN_PAYMENT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                }
                return;
            }

            // 2a) Aktywne ulepszenie — bot decyduje szybko, czlowiek ma 15s
            if (s.getPendingUpgradePos() != null) {
                int snapUpgradePos = s.getPendingUpgradePos();
                Long snapUpgradePlayer = s.getPendingUpgradePlayerId();
                if (snapUpgradePlayer == null) return;
                GamePlayer upgrader = s.getPlayers().stream()
                        .filter(p -> p.getId().equals(snapUpgradePlayer))
                        .findFirst().orElse(null);
                if (upgrader == null) return;
                if (upgrader.getUser() == null) {
                    scheduleOnce(sessionId,
                            () -> gameService.botUpgradeDecide(sessionId, snapUpgradePlayer, snapUpgradePos),
                            BOT_DECISION_DELAY_MS, TimeUnit.MILLISECONDS);
                } else {
                    scheduleOnce(sessionId,
                            () -> gameService.autoUpgradeTimeout(sessionId, snapUpgradePos, snapUpgradePlayer),
                            HUMAN_UPGRADE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                }
                return;
            }

            // 2b) Aktywna decyzja kupna - zaplanuj akcje decydenta
            if (s.getPendingPurchasePos() != null) {
                Long deciderId = s.getPendingDeciderId();
                if (deciderId == null) return;
                GamePlayer decider = s.getPlayers().stream()
                        .filter(p -> p.getId().equals(deciderId))
                        .findFirst().orElse(null);
                if (decider == null) return;
                int snapPos = s.getPendingPurchasePos();
                if (decider.getUser() == null) {
                    scheduleOnce(sessionId,
                            () -> gameService.botDecide(sessionId, decider.getId(), snapPos),
                            BOT_DECISION_DELAY_MS, TimeUnit.MILLISECONDS);
                } else {
                    scheduleOnce(sessionId,
                            () -> gameService.autoSkipTimeout(sessionId, snapPos),
                            HUMAN_DECISION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                }
                return;
            }

            // 3) Brak decyzji - jesli aktywny gracz to bot, rzuc za niego
            int turnIdx = activeTurnIndex(s.getPlayers(), s.getCurrentTurn());
            GamePlayer current = s.getPlayers().get(turnIdx);
            if (current.getUser() == null && !current.isBankrupt()) {
                Long botId = current.getId();
                scheduleOnce(sessionId,
                        () -> gameService.rollAsBot(sessionId, botId),
                        BOT_ROLL_DELAY_MS, TimeUnit.MILLISECONDS);
            }
        } catch (Exception ex) {
            log.warn("onTurnUpdate failed for session {}: {}", sessionId, ex.getMessage());
            scheduleRecovery(sessionId);
        }
    }

    /** Indeks pierwszego niebankruta od biezacej tury (jak normalizeCurrentTurn, bez zapisu). */
    private static int activeTurnIndex(List<GamePlayer> players, int currentTurn) {
        int n = players.size();
        if (n == 0) return 0;
        int start = currentTurn % n;
        for (int i = 0; i < n; i++) {
            int idx = (start + i) % n;
            if (!players.get(idx).isBankrupt()) return idx;
        }
        return start;
    }

    private void scheduleOnce(Long sessionId, Runnable task, long delay, TimeUnit unit) {
        lastActivity.put(sessionId, System.currentTimeMillis());
        cancelPending(sessionId);
        ScheduledFuture<?> future = scheduler.schedule(() -> {
            pendingTasks.remove(sessionId);
            safeCall(sessionId, task);
        }, delay, unit);
        pendingTasks.put(sessionId, future);
    }

    /** Ponowna proba gdy lancuch botow sie urwal (null return / wyjatek). */
    private void scheduleRecovery(Long sessionId) {
        scheduler.schedule(() -> {
            try {
                // przez proxy — onTurnUpdate wymaga @Transactional (lazy players/user)
                selfProvider.getObject().onTurnUpdate(sessionId);
            } catch (Exception ex) {
                log.warn("Bot recovery failed for session {}: {}", sessionId, ex.getMessage());
            }
        }, BOT_RECOVERY_DELAY_MS, TimeUnit.MILLISECONDS);
    }

    private void clearTracking(Long sessionId) {
        lastActivity.remove(sessionId);
        idleCycles.remove(sessionId);
    }

    private void cancelPending(Long sessionId) {
        ScheduledFuture<?> existing = pendingTasks.remove(sessionId);
        if (existing != null) {
            existing.cancel(false);
        }
    }

    private void safeCall(Long sessionId, Runnable r) {
        try {
            r.run();
        } catch (Exception ex) {
            log.warn("Bot action failed for session {}: {}", sessionId, ex.getMessage());
            scheduleRecovery(sessionId);
        }
    }
}
