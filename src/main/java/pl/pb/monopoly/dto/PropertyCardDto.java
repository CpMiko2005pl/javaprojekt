package pl.pb.monopoly.dto;

/** Szczegóły posesji gracza — panel „Moje nieruchomości” na planszy. */
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
