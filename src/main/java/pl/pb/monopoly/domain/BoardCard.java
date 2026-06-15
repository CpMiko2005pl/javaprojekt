package pl.pb.monopoly.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Karta planszowa losowana z pol Szansy / Kasy Studenckiej.
 * Tresc kart byla wpisana na sztywno (CHANCE_CARDS w GameService) — tutaj
 * przeniesiona do tabeli board_cards.
 */
@Entity
@Table(name = "board_cards")
public class BoardCard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Rodzaj karty, np. SZANSA, KASA_MIEJSKA, WYDARZENIE. */
    @NotBlank
    @Size(max = 20)
    @Column(name = "card_type", nullable = false, length = 20)
    private String cardType;

    @NotBlank
    @Size(max = 80)
    @Column(nullable = false, length = 80)
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    /** Efekt finansowy w PLN (dodatni = nagroda, ujemny = kara). */
    @Column(name = "money_effect", nullable = false)
    private int moneyEffect = 0;

    @Size(max = 80)
    @Column(name = "img_slug", length = 80)
    private String imgSlug;

    public BoardCard() {
    }

    public BoardCard(String cardType, String title, String description, int moneyEffect, String imgSlug) {
        this.cardType = cardType;
        this.title = title;
        this.description = description;
        this.moneyEffect = moneyEffect;
        this.imgSlug = imgSlug;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCardType() {
        return cardType;
    }

    public void setCardType(String cardType) {
        this.cardType = cardType;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getMoneyEffect() {
        return moneyEffect;
    }

    public void setMoneyEffect(int moneyEffect) {
        this.moneyEffect = moneyEffect;
    }

    public String getImgSlug() {
        return imgSlug;
    }

    public void setImgSlug(String imgSlug) {
        this.imgSlug = imgSlug;
    }
}
