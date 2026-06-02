package pl.pb.monopoly.domain;

/**
 * Role uzytkownikow w systemie (wymaganie: co najmniej 3 role, w tym administrator).
 * Zgodnie z deklaracja projektowa: Admin, Moderator, Uzytkownik oraz Gosc.
 *
 * Spring Security oczekuje autorytetow z prefiksem "ROLE_" - dodajemy go w
 * {@code authority()}, a w konfiguracji uzywamy nazw bez prefiksu (hasRole).
 */
public enum Role {

    ADMIN("Administrator"),
    MODERATOR("Moderator"),
    USER("Uzytkownik"),
    GUEST("Gosc");

    private final String displayName;

    Role(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /** Nazwa autorytetu wykorzystywana przez Spring Security (np. ROLE_ADMIN). */
    public String authority() {
        return "ROLE_" + name();
    }
}
