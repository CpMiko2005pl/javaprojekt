package pl.pb.monopoly.domain;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    private int cash = 2_000_000;

    @Column(nullable = false)
    private int position = 0;

    @Column(nullable = false, length = 7)
    private String color = "#e63946";

    @Column(nullable = false)
    private boolean bankrupt = false;

    @Column(name = "turn_order", nullable = false)
    private int turnOrder = 0;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "game_player_properties",
            joinColumns = @JoinColumn(name = "player_id"))
    @Column(name = "position")
    private Set<Integer> ownedPositions = new HashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "game_player_property_levels",
            joinColumns = @JoinColumn(name = "player_id"))
    @MapKeyColumn(name = "tile_position")
    @Column(name = "level")
    private Map<Integer, Integer> propertyLevels = new HashMap<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "game_player_landing_counts",
            joinColumns = @JoinColumn(name = "player_id"))
    @MapKeyColumn(name = "tile_position")
    @Column(name = "landing_count")
    private Map<Integer, Integer> landingCounts = new HashMap<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "game_player_hand_cards",
            joinColumns = @JoinColumn(name = "player_id"))
    @Column(name = "card_type")
    private List<String> handCards = new ArrayList<>();

    @Column(name = "ready", nullable = false, columnDefinition = "boolean default false")
    private boolean ready = false;

    @Column(name = "doubles_count", nullable = false, columnDefinition = "integer default 0")
    private int doublesCount = 0;

    @Column(name = "rolls_this_turn", nullable = false, columnDefinition = "integer default 0")
    private int rollsThisTurn = 0;

    @Column(name = "skip_next_rent", nullable = false)
    private boolean skipNextRent = false;

    @Column(name = "shield_active", nullable = false)
    private boolean shieldActive = false;

    @Column(name = "double_rent_next", nullable = false, columnDefinition = "boolean default false")
    private boolean doubleRentNext = false;

    @Column(name = "jail_pass_active", nullable = false, columnDefinition = "boolean default false")
    private boolean jailPassActive = false;

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

    public boolean isReady() { return ready; }
    public void setReady(boolean ready) { this.ready = ready; }

    public int getDoublesCount() { return doublesCount; }
    public void setDoublesCount(int doublesCount) { this.doublesCount = doublesCount; }

    public int getRollsThisTurn() { return rollsThisTurn; }
    public void setRollsThisTurn(int rollsThisTurn) { this.rollsThisTurn = rollsThisTurn; }

    public boolean isSkipNextRent() { return skipNextRent; }
    public void setSkipNextRent(boolean skipNextRent) { this.skipNextRent = skipNextRent; }

    public boolean isShieldActive() { return shieldActive; }
    public void setShieldActive(boolean shieldActive) { this.shieldActive = shieldActive; }

    public boolean isDoubleRentNext() { return doubleRentNext; }
    public void setDoubleRentNext(boolean doubleRentNext) { this.doubleRentNext = doubleRentNext; }

    public boolean isJailPassActive() { return jailPassActive; }
    public void setJailPassActive(boolean jailPassActive) { this.jailPassActive = jailPassActive; }
}
