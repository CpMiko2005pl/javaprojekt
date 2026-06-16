package pl.pb.monopoly.domain;

public enum AchievementType {

    FIRST_GAME("Debiut", "Rozegraj swoj pierwszy mecz", 100, "fa-solid fa-flag-checkered"),
    FIRST_WIN("Pierwsze zwyciestwo", "Wygraj swoj pierwszy mecz", 500, "fa-solid fa-trophy"),
    GAMES_10("Weteran kampusu", "Rozegraj 10 meczow", 500, "fa-solid fa-dice"),
    WINS_5("Seryjny zwyciezca", "Wygraj 5 meczow", 1000, "fa-solid fa-medal"),
    STREAK_3("Hat-trick", "Wygraj 3 mecze z rzedu", 750, "fa-solid fa-fire");

    public final String title;
    public final String description;
    public final int coinReward;
    public final String iconClass;

    AchievementType(String title, String description, int coinReward, String iconClass) {
        this.title = title;
        this.description = description;
        this.coinReward = coinReward;
        this.iconClass = iconClass;
    }

    public boolean isUnlocked(int gamesPlayed, int gamesWon, int winStreak) {
        return switch (this) {
            case FIRST_GAME -> gamesPlayed >= 1;
            case FIRST_WIN -> gamesWon >= 1;
            case GAMES_10 -> gamesPlayed >= 10;
            case WINS_5 -> gamesWon >= 5;
            case STREAK_3 -> winStreak >= 3;
        };
    }
}
