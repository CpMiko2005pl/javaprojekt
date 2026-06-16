package pl.pb.monopoly.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "match_history")
public class MatchHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "played_at", nullable = false)
    private LocalDateTime playedAt = LocalDateTime.now();

    @Column(name = "board_name", length = 60)
    private String boardName = "Kampus PB";

    @Column(nullable = false)
    private boolean won;

    @Column(nullable = false)
    private int placement;

    @Column(name = "players_count", nullable = false)
    private int playersCount;

    @Column(name = "final_cash", nullable = false)
    private int finalCash;

    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes;

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
