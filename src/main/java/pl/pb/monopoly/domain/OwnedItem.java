package pl.pb.monopoly.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "owned_items", indexes = {
        @Index(name = "idx_owned_user", columnList = "user_id")
})
public class OwnedItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "item_slug", nullable = false, length = 60)
    private String itemSlug;

    @Column(name = "obtained_at", nullable = false)
    private LocalDateTime obtainedAt = LocalDateTime.now();

    @Column(nullable = false)
    private boolean equipped = false;

    public OwnedItem() {
    }

    public OwnedItem(User user, String itemSlug) {
        this.user = user;
        this.itemSlug = itemSlug;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getItemSlug() { return itemSlug; }
    public void setItemSlug(String itemSlug) { this.itemSlug = itemSlug; }
    public LocalDateTime getObtainedAt() { return obtainedAt; }
    public void setObtainedAt(LocalDateTime obtainedAt) { this.obtainedAt = obtainedAt; }
    public boolean isEquipped() { return equipped; }
    public void setEquipped(boolean equipped) { this.equipped = equipped; }
}
