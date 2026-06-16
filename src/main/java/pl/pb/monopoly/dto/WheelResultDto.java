package pl.pb.monopoly.dto;

public record WheelResultDto(
        boolean spun,
        int rewardIndex,
        String rewardLabel,
        int dailyStreak,
        String message
) {
}
