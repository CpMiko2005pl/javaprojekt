package pl.pb.monopoly.domain;

import jakarta.persistence.*;

/**
 * Gracz w obrebie jednej sesji rozgrywki. Tu zyje "siano" (gotowka w grze) -
 * zgodnie z zalozeniem, ze saldo istnieje TYLKO podczas rozgrywki (jak w Monopoly).
 * Pozycja to numer pola na planszy (0-39).
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

    /** Nazwa wyswietlana pionka (gdy gracz to bot, user moze byc null). */
    @Column(nullable = false, length = 30)
    private String displayName;

    /** "Siano" - gotowka w grze (PLN). Start: 1500. */
    @Column(nullable = false)
    private int cash = 1500;

    /** Pozycja pionka na planszy: 0-39. */
    @Column(nullable = false)
    private int position = 0;

    /** Kolor pionka (hex) do rysowania na canvasie. */
    @Column(nullable = false, length = 7)
    private String color = "#e63946";

    @Column(nullable = false)
    private boolean bankrupt = false;

    /** Kolejnosc ruchu w sesji. */
    @Column(name = "turn_order", nullable = false)
    private int turnOrder = 0;

    public GamePlayer() {
    }

    public GamePlayer(String displayName, String color) {
        this.displayName = displayName;
        this.color = color;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public GameSession getSession() {
        return session;
    }

    public void setSession(GameSession session) {
        this.session = session;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public int getCash() {
        return cash;
    }

    public void setCash(int cash) {
        this.cash = cash;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public boolean isBankrupt() {
        return bankrupt;
    }

    public void setBankrupt(boolean bankrupt) {
        this.bankrupt = bankrupt;
    }

    public int getTurnOrder() {
        return turnOrder;
    }

    public void setTurnOrder(int turnOrder) {
        this.turnOrder = turnOrder;
    }
}
