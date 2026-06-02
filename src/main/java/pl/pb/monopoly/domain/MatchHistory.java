package pl.pb.monopoly.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Wpis historii rozegranych meczow (do panelu gracza w stylu FACEIT).
 * Powiazany z {@link User} relacja *-1.
 */
@Entity
@Table(name = "match_history")
public class MatchHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Pole typu Date - kiedy rozegrano mecz. */
    @Column(name = "played_at", nullable = false)
    private LocalDateTime playedAt = LocalDateTime.now();

    /** Nazwa planszy/mapy, np. "Kampus PB". */
    @Column(name = "board_name", length = 60)
    private String boardName = "Kampus PB";

    /** Czy gracz wygral mecz. */
    @Column(nullable = false)
    private boolean won;

    /** Miejsce w koncowej klasyfikacji (1 = zwyciezca). */
    @Column(nullable = false)
    private int placement;

    /** Liczba graczy w meczu. */
    @Column(name = "players_count", nullable = false)
    private int playersCount;

    /** Stan "siana" gracza na koniec meczu. */
    @Column(name = "final_cash", nullable = false)
    private int finalCash;

    /** Czas trwania meczu w minutach. */
    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes;

    /** Zmiana punktow ELO po meczu (+/-). */
    @Column(name = "elo_change", nullable = false)
    private int eloChange;

    public MatchHistory() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public LocalDateTime getPlayedAt() {
        return playedAt;
    }

    public void setPlayedAt(LocalDateTime playedAt) {
        this.playedAt = playedAt;
    }

    public String getBoardName() {
        return boardName;
    }

    public void setBoardName(String boardName) {
        this.boardName = boardName;
    }

    public boolean isWon() {
        return won;
    }

    public void setWon(boolean won) {
        this.won = won;
    }

    public int getPlacement() {
        return placement;
    }

    public void setPlacement(int placement) {
        this.placement = placement;
    }

    public int getPlayersCount() {
        return playersCount;
    }

    public void setPlayersCount(int playersCount) {
        this.playersCount = playersCount;
    }

    public int getFinalCash() {
        return finalCash;
    }

    public void setFinalCash(int finalCash) {
        this.finalCash = finalCash;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(int durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public int getEloChange() {
        return eloChange;
    }

    public void setEloChange(int eloChange) {
        this.eloChange = eloChange;
    }
}
