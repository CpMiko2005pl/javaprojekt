package pl.pb.monopoly.dto;

/** Stan oczekujacego ulepszenia nieruchomosci (2. wizyta wlasciciela na polu). */
public record PendingUpgradeDto(
        int position,
        String tileName,
        int cost,
        int currentLevel,
        int newRent,
        Long deciderId
) {}
