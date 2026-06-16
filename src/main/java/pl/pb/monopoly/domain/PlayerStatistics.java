package pl.pb.monopoly.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;

import java.time.LocalDate;

@Entity
@Table(name = "player_statistics")
public class PlayerStatistics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Min(0)
    @Column(name = "games_played", nullable = false)
    private int gamesPlayed = 0;

    @Min(0)
    @Column(name = "games_won", nullable = false)
    private int gamesWon = 0;

    @Min(1)
    @Column(nullable = false)
    private int level = 1;

    @Min(0)
    @Column(name = "elo_points", nullable = false)
    private int eloPoints = 1000;

    @Min(0)
    @Column(name = "win_streak", nullable = false)
    private int winStreak = 0;

    @Min(0)
    @Column(name = "daily_streak", nullable = false)
    private int dailyStreak = 0;

    @Column(name = "last_login")
    private LocalDate lastLogin;

    @Column(name = "last_spin_date")
    private LocalDate lastSpinDate;

    @Column(name = "last_reward", length = 120)
    private String lastReward;

    @Column(name = "pending_wheel_card", length = 40)
    private String pendingWheelCard;

    @Min(0)
    @Column(name = "pending_start_cash_bonus", nullable = false)
    private int pendingStartCashBonus = 0;

    @Min(0)
    @Column(name = "available_lootboxes", nullable = false)
    private int availableLootboxes = 1;

    @Column(name = "last_lootbox_grant_date")
    private LocalDate lastLootboxGrantDate;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    public PlayerStatistics() {
    }

    @Transient
    public int getWinRate() {
        return gamesPlayed == 0 ? 0 : Math.round(gamesWon * 100f / gamesPlayed);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public int getGamesPlayed() {
        return gamesPlayed;
    }

    public void setGamesPlayed(int gamesPlayed) {
        this.gamesPlayed = gamesPlayed;
    }

    public int getGamesWon() {
        return gamesWon;
    }

    public void setGamesWon(int gamesWon) {
        this.gamesWon = gamesWon;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public int getEloPoints() {
        return eloPoints;
    }

    public void setEloPoints(int eloPoints) {
        this.eloPoints = eloPoints;
    }

    public int getWinStreak() {
        return winStreak;
    }

    public void setWinStreak(int winStreak) {
        this.winStreak = winStreak;
    }

    public int getDailyStreak() {
        return dailyStreak;
    }

    public void setDailyStreak(int dailyStreak) {
        this.dailyStreak = dailyStreak;
    }

    public LocalDate getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(LocalDate lastLogin) {
        this.lastLogin = lastLogin;
    }

    public LocalDate getLastSpinDate() {
        return lastSpinDate;
    }

    public void setLastSpinDate(LocalDate lastSpinDate) {
        this.lastSpinDate = lastSpinDate;
    }

    public String getLastReward() {
        return lastReward;
    }

    public void setLastReward(String lastReward) {
        this.lastReward = lastReward;
    }

    public String getPendingWheelCard() {
        return pendingWheelCard;
    }

    public void setPendingWheelCard(String pendingWheelCard) {
        this.pendingWheelCard = pendingWheelCard;
    }

    public int getPendingStartCashBonus() {
        return pendingStartCashBonus;
    }

    public void setPendingStartCashBonus(int pendingStartCashBonus) {
        this.pendingStartCashBonus = pendingStartCashBonus;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public int getAvailableLootboxes() {
        return availableLootboxes;
    }

    public void setAvailableLootboxes(int availableLootboxes) {
        this.availableLootboxes = availableLootboxes;
    }

    public LocalDate getLastLootboxGrantDate() {
        return lastLootboxGrantDate;
    }

    public void setLastLootboxGrantDate(LocalDate lastLootboxGrantDate) {
        this.lastLootboxGrantDate = lastLootboxGrantDate;
    }
}
