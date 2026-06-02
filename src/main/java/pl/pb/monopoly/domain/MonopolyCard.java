package pl.pb.monopoly.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Karta Monopoly (Szansa / Kasa miejska / Wydarzenie).
 * Niezalezna encja slownikowa - tresc kart definiuje admin/moderator.
 */
@Entity
@Table(name = "monopoly_cards")
public class MonopolyCard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(min = 3, max = 60)
    @Column(nullable = false, length = 60)
    private String title;

    @NotBlank
    @Size(min = 5, max = 255)
    @Column(nullable = false, length = 255)
    private String description;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CardType type;

    /**
     * Efekt finansowy karty w PLN (moze byc ujemny - kara, lub dodatni - nagroda).
     * Np. +200 PLN "Otrzymujesz stypendium", -100 PLN "Mandat za rower na deptaku".
     */
    @Column(name = "money_effect", nullable = false)
    private int moneyEffect = 0;

    public MonopolyCard() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public CardType getType() {
        return type;
    }

    public void setType(CardType type) {
        this.type = type;
    }

    public int getMoneyEffect() {
        return moneyEffect;
    }

    public void setMoneyEffect(int moneyEffect) {
        this.moneyEffect = moneyEffect;
    }
}
