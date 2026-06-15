package pl.pb.monopoly.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Zadanie dzienne — cel do wykonania za nagrode (monety / XP / skrzynka).
 * Przeniesione do tabeli daily_tasks, dzieki czemu liste zadan mozna edytowac
 * bez zmian w kodzie.
 */
@Entity
@Table(name = "daily_tasks")
public class DailyTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 80)
    @Column(nullable = false, length = 80)
    private String name;

    @Size(max = 255)
    @Column(length = 255)
    private String description;

    /** Typ nagrody, np. COINS, XP, LOOTBOX. */
    @NotBlank
    @Size(max = 30)
    @Column(name = "reward_type", nullable = false, length = 30)
    private String rewardType;

    @Column(name = "reward_value", nullable = false)
    private int rewardValue;

    @Column(nullable = false)
    private boolean active = true;

    public DailyTask() {
    }

    public DailyTask(String name, String description, String rewardType, int rewardValue, boolean active) {
        this.name = name;
        this.description = description;
        this.rewardType = rewardType;
        this.rewardValue = rewardValue;
        this.active = active;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getRewardType() {
        return rewardType;
    }

    public void setRewardType(String rewardType) {
        this.rewardType = rewardType;
    }

    public int getRewardValue() {
        return rewardValue;
    }

    public void setRewardValue(int rewardValue) {
        this.rewardValue = rewardValue;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
