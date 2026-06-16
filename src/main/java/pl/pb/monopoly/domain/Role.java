package pl.pb.monopoly.domain;

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

    public String authority() {
        return "ROLE_" + name();
    }
}
