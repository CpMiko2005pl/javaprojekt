package pl.pb.monopoly.dto;

public record PendingUpgradeDto(
        int position,
        String tileName,
        int cost,
        int currentLevel,
        int newRent,
        Long deciderId
) {}
