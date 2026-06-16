package pl.pb.monopoly.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.pb.monopoly.domain.GamePlayer;
import pl.pb.monopoly.domain.GameSession;
import pl.pb.monopoly.domain.HandCardType;
import pl.pb.monopoly.dto.GameStateDto;
import pl.pb.monopoly.repository.GameSessionRepository;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Service
public class BotAutoplayService {

    private static final Logger log = LoggerFactory.getLogger(BotAutoplayService.class);

    private static final long BOT_ROLL_DELAY_MS = 1500;

    private static final long BOT_DECISION_DELAY_MS = 4200;

    private static final long HUMAN_DECISION_TIMEOUT_SECONDS = 25;

    private static final long HUMAN_PAYMENT_TIMEOUT_SECONDS = 30;

    private static final long HUMAN_UPGRADE_TIMEOUT_SECONDS = 15;

    private static final long HUMAN_TAKEOVER_TIMEOUT_SECONDS = 15;

    private static final long BOT_RECOVERY_DELAY_MS = 600;

    private static final long WATCHDOG_ACTIVE_WINDOW_MS = 90_000;

    private final GameSessionRepository sessionRepository;
    private final GameService gameService;

    private final ActiveGameStore activeGameStore;

    private final ObjectProvider<BotAutoplayService> selfProvider;
    private final ScheduledExecutorService scheduler;

    private final ConcurrentHashMap<Long, ScheduledFuture<?>> pendingTasks = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<Long, Integer> idleCycles = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<Long, Long> lastActivity = new ConcurrentHashMap<>();

    public BotAutoplayService(GameSessionRepository sessionRepository,
                              @Lazy GameService gameService,
                              ActiveGameStore activeGameStore,
                              ObjectProvider<BotAutoplayService> selfProvider) {
        this.sessionRepository = sessionRepository;
        this.gameService = gameService;
        this.activeGameStore = activeGameStore;
        this.selfProvider = selfProvider;
        this.scheduler = Executors.newScheduledThreadPool(2, r -> {
            Thread t = new Thread(r, "bot-autoplay");
            t.setDaemon(true);
            return t;
        });

        this.scheduler.scheduleWithFixedDelay(this::watchdog, 8, 5, TimeUnit.SECONDS);
    }

    private void watchdog() {
        try {
            long now = System.currentTimeMillis();

            lastActivity.entrySet().removeIf(e -> now - e.getValue() > WATCHDOG_ACTIVE_WINDOW_MS);
            for (Long id : new java.util.ArrayList<>(lastActivity.keySet())) {
                if (pendingTasks.containsKey(id)) {
                    idleCycles.remove(id);
                    continue;
                }
                int idle = idleCycles.merge(id, 1, Integer::sum);
                if (idle >= 2) {
                    idleCycles.remove(id);
                    log.info("Watchdog: odblokowuje sesje {} (brak akcji bota)", id);

                    selfProvider.getObject().onTurnUpdate(id);
                }
            }
            idleCycles.keySet().retainAll(lastActivity.keySet());
        } catch (Exception ex) {
            log.warn("Watchdog failed: {}", ex.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public void onTurnUpdate(Long sessionId) {
        if (sessionId == null) return;
        try {

            GameSession s = activeGameStore.get(sessionId);
            if (s == null) { clearTracking(sessionId); return; }
            if (s.getPlayers().isEmpty()) return;
            if (s.getStatus() == pl.pb.monopoly.domain.GameStatus.FINISHED) { clearTracking(sessionId); return; }
            if (s.getStatus() == pl.pb.monopoly.domain.GameStatus.WAITING) { clearTracking(sessionId); return; }
            lastActivity.put(sessionId, System.currentTimeMillis());

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

            if (s.getPendingTakeoverPos() != null) {
                Long buyerId = s.getPendingTakeoverBuyerId();
                if (buyerId == null) return;
                GamePlayer buyer = s.getPlayers().stream()
                        .filter(p -> p.getId().equals(buyerId))
                        .findFirst().orElse(null);
                if (buyer == null) return;
                int snapTakeoverPos = s.getPendingTakeoverPos();
                if (buyer.getUser() == null) {
                    scheduleOnce(sessionId,
                            () -> gameService.resolveTakeoverAsBot(sessionId, buyerId, snapTakeoverPos),
                            BOT_DECISION_DELAY_MS, TimeUnit.MILLISECONDS);
                } else {
                    scheduleOnce(sessionId,
                            () -> gameService.autoTakeoverTimeout(sessionId, snapTakeoverPos),
                            HUMAN_TAKEOVER_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                }
                return;
            }

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

            int turnIdx = activeTurnIndex(s.getPlayers(), s.getCurrentTurn());
            GamePlayer current = s.getPlayers().get(turnIdx);
            if (current.getUser() == null && !current.isBankrupt()) {
                Long botId = current.getId();

                String cardToPlay = chooseBotCard(current, s);
                if (cardToPlay != null) {
                    final String card = cardToPlay;
                    scheduleOnce(sessionId,
                            () -> {
                                GameStateDto result = gameService.playCard(sessionId, botId, card);
                                if (result == null) {

                                    gameService.rollAsBot(sessionId, botId);
                                }
                            },
                            BOT_DECISION_DELAY_MS, TimeUnit.MILLISECONDS);
                } else {
                    scheduleOnce(sessionId,
                            () -> gameService.rollAsBot(sessionId, botId),
                            BOT_ROLL_DELAY_MS, TimeUnit.MILLISECONDS);
                }
            }
        } catch (Exception ex) {
            log.warn("onTurnUpdate failed for session {}: {}", sessionId, ex.getMessage());
            scheduleRecovery(sessionId);
        }
    }

    private static String chooseBotCard(GamePlayer bot, GameSession s) {
        List<String> hand = bot.getHandCards();
        if (hand == null || hand.isEmpty()) return null;

        for (String c : hand) {
            if (HandCardType.EXTRA_ROLL.name().equals(c)) return c;
        }
        for (String c : hand) {
            if (HandCardType.SCHOLARSHIP_ALL.name().equals(c)) return c;
        }
        for (String c : hand) {
            if (HandCardType.ADD_CASH.name().equals(c) && bot.getCash() < 500_000) return c;
        }
        for (String c : hand) {
            if (HandCardType.DOUBLE_RENT_NEXT.name().equals(c)) return c;
        }
        return null;
    }

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

    private void scheduleRecovery(Long sessionId) {
        scheduler.schedule(() -> {
            try {

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
