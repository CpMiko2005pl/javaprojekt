package pl.pb.monopoly.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Wpis w historii/logach rozgrywek danego gracza.
 * KOMPOZYCJA z {@link User} - log nie istnieje bez gracza (usuwany kaskadowo).
 */
@Entity
@Table(name = "game_logs")
public class GameLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Opis zdarzenia, np. "Kupiono: Biblioteka PB za 220 PLN". */
    @Column(nullable = false, length = 255)
    private String action;

    /** Wynik/saldo po zdarzeniu lub krotki status. */
    @Column(length = 100)
    private String result;

    /** Pole typu Date - kiedy zdarzenie wystapilo. */
    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    public GameLog() {
    }

    public GameLog(String action, String result, User user) {
        this.action = action;
        this.result = result;
        this.user = user;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(LocalDateTime occurredAt) {
        this.occurredAt = occurredAt;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
