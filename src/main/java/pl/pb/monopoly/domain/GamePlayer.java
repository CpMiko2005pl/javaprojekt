package pl.pb.monopoly.domain;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Gracz w obrebie jednej sesji rozgrywki.
 * - cash: "siano" (gotowka w grze)
 * - ownedPositions: posiadane pola
 * - propertyLevels: poziomy ulepszenia pol (0=brak, 1=domek, 2=hotel)
 * - landingCounts: ile razy wlasciciel SZNUROWL na WLASNYM polu (do oferty ulepszenia)
 * - handCards: karty w rece (maks. 3, nazwy HandCardType)
 * - skipNextRent: karta "Ochrona" jest aktywna
 * - shieldActive: karta "Tarcza Akademicka" jest aktywna
 */
@Entity
@Table(name = "game_players")
public class GamePlayer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private GameSession session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false, length = 30)
    private String displayName;

    @Column(nullable = false)
    private int cash = 1500;

    @Column(nullable = false)
    private int position = 0;

    @Column(nullable = false, length = 7)
    private String color = "#e63946";

    @Column(nullable = false)
    private boolean bankrupt = false;

    @Column(name = "turn_order", nullable = false)
    private int turnOrder = 0;

    /** Pola planszy, ktore gracz posiada (pozycje 0-39). */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "game_player_properties",
            joinColumns = @JoinColumn(name = "player_id"))
    @Column(name = "position")
    private Set<Integer> ownedPositions = new HashSet<>();

    /** Poziomy ulepszenia pol: pozycja -> poziom (0=brak, 1=domek, 2=hotel). */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "game_player_property_levels",
            joinColumns = @JoinColumn(name = "player_id"))
    @MapKeyColumn(name = "tile_position")
    @Column(name = "level")
    private Map<Integer, Integer> propertyLevels = new HashMap<>();

    /** Ile razy wlasciciel stanął na WLASNYM polu (do logiki ulepszenia). */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "game_player_landing_counts",
            joinColumns = @JoinColumn(name = "player_id"))
    @MapKeyColumn(name = "tile_position")
    @Column(name = "landing_count")
    private Map<Integer, Integer> landingCounts = new HashMap<>();

    /** Karty w rece (HandCardType.name()), maks. 3 na gre. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "game_player_hand_cards",
            joinColumns = @JoinColumn(name = "player_id"))
    @Column(name = "card_type")
    private List<String> handCards = new ArrayList<>();

    /** Karta "Karta Ochrony" jest aktywna — nastepny czynsz jest pomijany. */
    @Column(name = "skip_next_rent", nullable = false)
    private boolean skipNextRent = false;

    /** Karta "Tarcza Akademicka" jest aktywna. */
    @Column(name = "shield_active", nullable = false)
    private boolean shieldActive = false;

    public GamePlayer() {
    }

    public GamePlayer(String displayName, String color) {
        this.displayName = displayName;
        this.color = color;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public GameSession getSession() { return session; }
    public void setSession(GameSession session) { this.session = session; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public int getCash() { return cash; }
    public void setCash(int cash) { this.cash = cash; }

    public int getPosition() { return position; }
    public void setPosition(int position) { this.position = position; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public boolean isBankrupt() { return bankrupt; }
    public void setBankrupt(boolean bankrupt) { this.bankrupt = bankrupt; }

    public int getTurnOrder() { return turnOrder; }
    public void setTurnOrder(int turnOrder) { this.turnOrder = turnOrder; }

    public Set<Integer> getOwnedPositions() { return ownedPositions; }
    public void setOwnedPositions(Set<Integer> ownedPositions) { this.ownedPositions = ownedPositions; }

    public Map<Integer, Integer> getPropertyLevels() { return propertyLevels; }
    public void setPropertyLevels(Map<Integer, Integer> propertyLevels) { this.propertyLevels = propertyLevels; }

    public Map<Integer, Integer> getLandingCounts() { return landingCounts; }
    public void setLandingCounts(Map<Integer, Integer> landingCounts) { this.landingCounts = landingCounts; }

    public List<String> getHandCards() { return handCards; }
    public void setHandCards(List<String> handCards) { this.handCards = handCards; }

    public boolean isSkipNextRent() { return skipNextRent; }
    public void setSkipNextRent(boolean skipNextRent) { this.skipNextRent = skipNextRent; }

    public boolean isShieldActive() { return shieldActive; }
    public void setShieldActive(boolean shieldActive) { this.shieldActive = shieldActive; }
}
