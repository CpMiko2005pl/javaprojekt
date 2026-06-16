package pl.pb.monopoly.dto;

import java.time.LocalDateTime;

public record AchievementView(
        String title,
        String description,
        int coinReward,
        String iconClass,
        boolean unlocked,
        LocalDateTime unlockedAt
) {}
