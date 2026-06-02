package pl.pb.monopoly.domain;

/**
 * Rodzaj karty Monopoly. W wersji PB:
 *  - SZANSA       -> odpowiednik "Szansy"
 *  - KASA_MIEJSKA -> odpowiednik "Kasy spolecznej"
 *  - WYDARZENIE   -> karta wydarzenia wplywajaca globalnie na plansze
 */
public enum CardType {
    SZANSA("Szansa"),
    KASA_MIEJSKA("Kasa miejska"),
    WYDARZENIE("Wydarzenie");

    private final String displayName;

    CardType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
