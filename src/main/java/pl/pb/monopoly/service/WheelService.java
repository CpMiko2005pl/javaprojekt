package pl.pb.monopoly.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.pb.monopoly.domain.HandCardType;
import pl.pb.monopoly.domain.PlayerStatistics;
import pl.pb.monopoly.domain.User;
import pl.pb.monopoly.dto.WheelResultDto;
import pl.pb.monopoly.repository.UserRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class WheelService {

    private record WheelSlot(String label, WheelRewardType type) {}

    private enum WheelRewardType {
        CARD, LOOTBOX, START_CASH
    }

    private static final List<WheelSlot> SLOTS = List.of(
            new WheelSlot(HandCardType.SKIP_RENT.label, WheelRewardType.CARD),
            new WheelSlot(HandCardType.EXTRA_ROLL.label, WheelRewardType.CARD),
            new WheelSlot(HandCardType.DESTROY_PROPERTY.label, WheelRewardType.CARD),
            new WheelSlot(HandCardType.ADD_CASH.label, WheelRewardType.CARD),
            new WheelSlot(HandCardType.SHIELD.label, WheelRewardType.CARD),
            new WheelSlot(HandCardType.FREE_UPGRADE.label, WheelRewardType.CARD),
            new WheelSlot(HandCardType.JAIL_PASS.label, WheelRewardType.CARD),
            new WheelSlot(HandCardType.DOUBLE_RENT_NEXT.label, WheelRewardType.CARD),
            new WheelSlot(HandCardType.TELEPORT.label, WheelRewardType.CARD),
            new WheelSlot(HandCardType.SCHOLARSHIP_ALL.label, WheelRewardType.CARD),
            new WheelSlot("+1 Skrzynka", WheelRewardType.LOOTBOX),
            new WheelSlot("+200 000 PLN na start gry", WheelRewardType.START_CASH),
            new WheelSlot("+1 Skrzynka (bonus)", WheelRewardType.LOOTBOX)
    );

    private static final HandCardType[] CARD_POOL = HandCardType.values();

    private final UserRepository userRepository;

    public WheelService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public static List<String> rewardLabels() {
        return SLOTS.stream().map(WheelSlot::label).toList();
    }

    public int segmentCount() {
        return SLOTS.size();
    }

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

        if (today.minusDays(1).equals(s.getLastSpinDate())) {
            s.setDailyStreak(s.getDailyStreak() + 1);
        } else {
            s.setDailyStreak(1);
        }

        int idx = ThreadLocalRandom.current().nextInt(SLOTS.size());
        WheelSlot slot = SLOTS.get(idx);
        String applied = applyReward(s, slot);

        s.setLastSpinDate(today);
        s.setLastReward(slot.label());

        return new WheelResultDto(true, idx, slot.label(), s.getDailyStreak(), applied);
    }

    private String applyReward(PlayerStatistics stats, WheelSlot slot) {
        return switch (slot.type()) {
            case CARD -> {
                HandCardType card = cardForLabel(slot.label());
                stats.setPendingWheelCard(card.name());
                yield "Gratulacje! W nastepnej grze dostaniesz karte: " + card.label + ".";
            }
            case LOOTBOX -> {
                stats.setAvailableLootboxes(Math.min(stats.getAvailableLootboxes() + 1, 5));
                yield "Gratulacje! +1 skrzynka dodana do zapasu (max 5).";
            }
            case START_CASH -> {
                stats.setPendingStartCashBonus(stats.getPendingStartCashBonus() + 200_000);
                yield "Gratulacje! +200 000 PLN na start nastepnej rozgrywki.";
            }
        };
    }

    private static HandCardType cardForLabel(String label) {
        for (HandCardType t : HandCardType.values()) {
            if (t.label.equals(label)) {
                return t;
            }
        }
        return CARD_POOL[ThreadLocalRandom.current().nextInt(CARD_POOL.length)];
    }
}
