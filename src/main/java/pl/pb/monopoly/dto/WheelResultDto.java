package pl.pb.monopoly.dto;

/**
 * Wynik zakrecenia codziennym Kolem Fortuny.
 * @param spun         czy udalo sie zakrecic (false = juz dzis losowano)
 * @param rewardIndex  indeks pola na kole (do animacji na froncie)
 * @param rewardLabel  opis wylosowanej nagrody/ulatwienia
 * @param dailyStreak  aktualna seria dni logowania
 * @param message      komunikat dla gracza
 */
public record WheelResultDto(
        boolean spun,
        int rewardIndex,
        String rewardLabel,
        int dailyStreak,
        String message
) {
}
