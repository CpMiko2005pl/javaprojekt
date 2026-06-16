package pl.pb.monopoly.dto;

public record PropertyCardDto(
        int position,
        String tileName,
        int buyPrice,
        int currentRent,
        int bankSellPrice,
        int upgradeCost,
        int nextRent,
        int level,
        String levelLabel,
        boolean canUpgradeFurther
) {
}
