package pl.pb.monopoly.service;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.pb.monopoly.domain.GamePlayer;
import pl.pb.monopoly.domain.GameSession;
import pl.pb.monopoly.repository.GameSessionRepository;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Automatyzacja: <ul>
 *   <li>boty rzucaja kostka 1.5s po przejsciu na ich ture,</li>
 *   <li>boty decyduja pendingPurchase (kupuja gdy cash &gt; 2x cena, inaczej skipuja) po 1.3s,</li>
 *   <li>czlowiek ma 10s na decyzje pendingPurchase - po tym czasie auto-skip.</li>
 * </ul>
 *
 * Wszystkie zaplanowane akcje wywoluja {@link GameService} ktore zapisuje stan i broadcastuje
 * przez WebSocket. {@link BotAutoplayService#onTurnUpdate(Long)} jest hookiem wolanym po kazdej
 * zmianie stanu by lancuchowo planowac kolejne akcje.
 */
@Service
public class BotAutoplayService {

    /** Opoznienie miedzy zakonczeniem stanu a rzutem bota - dramaturgia. */
    private static final long BOT_ROLL_DELAY_MS = 1500;
    /** Opoznienie miedzy postawieniem pendingPurchase a decyzja bota. */
    private static final long BOT_DECISION_DELAY_MS = 1300;
    /** Czas na decyzje gracza-czlowieka. */
    private static final long HUMAN_DECISION_TIMEOUT_SECONDS = 10;

    private final GameSessionRepository sessionRepository;
    private final GameService gameService;
    private final ScheduledExecutorService scheduler;

    public BotAutoplayService(GameSessionRepository sessionRepository,
                              @Lazy GameService gameService) {
        this.sessionRepository = sessionRepository;
        this.gameService = gameService;
        this.scheduler = Executors.newScheduledThreadPool(2, r -> {
            Thread t = new Thread(r, "bot-autoplay");
            t.setDaemon(true);
            return t;
        });
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

            // 1) Aktywna decyzja - zaplanuj akcje decydenta
            if (s.getPendingPurchasePos() != null) {
                Long deciderId = s.getPendingDeciderId();
                if (deciderId == null) return;
                GamePlayer decider = s.getPlayers().stream()
                        .filter(p -> p.getId().equals(deciderId))
                        .findFirst().orElse(null);
                if (decider == null) return;
                int snapPos = s.getPendingPurchasePos();
                if (decider.getUser() == null) {
                    // Bot decyduje (cala logika w GameService.botDecide w nowej transakcji)
                    scheduler.schedule(
                            () -> safeCall(() -> gameService.botDecide(sessionId, decider.getId(), snapPos)),
                            BOT_DECISION_DELAY_MS, TimeUnit.MILLISECONDS);
                } else {
                    // Czlowiek - 10s na decyzje, potem auto-skip
                    scheduler.schedule(
                            () -> safeCall(() -> gameService.autoSkipTimeout(sessionId, snapPos)),
                            HUMAN_DECISION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                }
                return;
            }

            // 2) Brak decyzji - jesli aktywny gracz to bot, rzuc za niego
            GamePlayer current = s.getPlayers().get(s.getCurrentTurn() % s.getPlayers().size());
            if (current.getUser() == null && !current.isBankrupt()) {
                Long botId = current.getId();
                scheduler.schedule(
                        () -> safeCall(() -> gameService.rollAsBot(sessionId, botId)),
                        BOT_ROLL_DELAY_MS, TimeUnit.MILLISECONDS);
            }
        } catch (Exception ignored) {
            // best-effort: bledy nie powinny wywalac silnika gry
        }
    }

    private void safeCall(Runnable r) {
        try {
            r.run();
        } catch (Exception ignored) {
        }
    }
}
