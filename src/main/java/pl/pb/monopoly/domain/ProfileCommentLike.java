package pl.pb.monopoly.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "profile_comment_likes",
        uniqueConstraints = @UniqueConstraint(name = "uk_pc_like_comment_user",
                columnNames = {"comment_id", "user_id"}))
public class ProfileCommentLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "comment_id", nullable = false)
    private ProfileComment comment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public ProfileCommentLike() {
    }

    public ProfileCommentLike(ProfileComment comment, User user) {
        this.comment = comment;
        this.user = user;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public ProfileComment getComment() {
        return comment;
    }

    public void setComment(ProfileComment comment) {
        this.comment = comment;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
