package pl.pb.monopoly.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Ranga gracza wyznaczana na podstawie punktow ELO (styl FACEIT).
 * Powiazana z PlayerStatistics przez kolumne rank_id w tabeli player_statistics.
 */
@Entity
@Table(name = "ranks")
public class Rank {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 40)
    @Column(nullable = false, length = 40)
    private String name;

    /** Dolny prog ELO (wlacznie). */
    @Column(name = "elo_from", nullable = false)
    private int eloFrom;

    /** Gorny prog ELO (wlacznie). */
    @Column(name = "elo_to", nullable = false)
    private int eloTo;

    @Size(max = 7)
    @Column(name = "badge_color", length = 7)
    private String badgeColor;

    @Size(max = 60)
    @Column(name = "icon_slug", length = 60)
    private String iconSlug;

    public Rank() {
    }

    public Rank(String name, int eloFrom, int eloTo, String badgeColor, String iconSlug) {
        this.name = name;
        this.eloFrom = eloFrom;
        this.eloTo = eloTo;
        this.badgeColor = badgeColor;
        this.iconSlug = iconSlug;
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

    public int getEloFrom() {
        return eloFrom;
    }

    public void setEloFrom(int eloFrom) {
        this.eloFrom = eloFrom;
    }

    public int getEloTo() {
        return eloTo;
    }

    public void setEloTo(int eloTo) {
        this.eloTo = eloTo;
    }

    public String getBadgeColor() {
        return badgeColor;
    }

    public void setBadgeColor(String badgeColor) {
        this.badgeColor = badgeColor;
    }

    public String getIconSlug() {
        return iconSlug;
    }

    public void setIconSlug(String iconSlug) {
        this.iconSlug = iconSlug;
    }
}
