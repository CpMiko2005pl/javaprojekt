package pl.pb.monopoly.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "game_invites")
public class GameInvite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private GameSession session;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inviter_id", nullable = false)
    private User inviter;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invitee_id", nullable = false)
    private User invitee;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private GameInviteStatus status = GameInviteStatus.PENDING;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public GameInvite() {
    }

    public GameInvite(GameSession session, User inviter, User invitee) {
        this.session = session;
        this.inviter = inviter;
        this.invitee = invitee;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public GameSession getSession() { return session; }
    public void setSession(GameSession session) { this.session = session; }

    public User getInviter() { return inviter; }
    public void setInviter(User inviter) { this.inviter = inviter; }

    public User getInvitee() { return invitee; }
    public void setInvitee(User invitee) { this.invitee = invitee; }

    public GameInviteStatus getStatus() { return status; }
    public void setStatus(GameInviteStatus status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
