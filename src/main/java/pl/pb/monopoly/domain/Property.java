package pl.pb.monopoly.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Nieruchomosc na planszy (pole, ktore mozna kupic) wraz z cena i czynszem.
 * To glowna encja dla pokazowego panelu CRUD (pelne dodawanie/edycja/usuwanie/lista).
 * Posiada wiecej niz 4 pola roznych typow z ograniczeniami wartosci.
 *
 * Wlasciciel (owner) jest opcjonalny - to relacja asocjacji (NIE kompozycji):
 * nieruchomosc istnieje niezaleznie od gracza, ktory akurat ja posiada.
 */
@Entity
@Table(name = "properties")
public class Property {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Nazwa jest wymagana")
    @Size(min = 3, max = 60, message = "Nazwa musi miec od 3 do 60 znakow")
    @Column(nullable = false, length = 60)
    private String name;

    @NotBlank(message = "Lokalizacja jest wymagana")
    @Size(min = 3, max = 80)
    @Column(nullable = false, length = 80)
    private String location;

    @NotNull(message = "Cena jest wymagana")
    @DecimalMin(value = "1.00", message = "Cena musi byc dodatnia")
    @DecimalMax(value = "100000.00", message = "Cena jest zbyt wysoka")
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @NotNull(message = "Czynsz jest wymagany")
    @DecimalMin(value = "0.00", message = "Czynsz nie moze byc ujemny")
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal rent;

    /** Kolor grupy nieruchomosci (jak w Monopoly), np. "Czerwony", "Granatowy". */
    @Size(max = 20)
    @Column(name = "color_group", length = 20)
    private String colorGroup;

    /** Liczba postawionych domkow (0-4) lub 5 = hotel. */
    @Min(0)
    @Max(5)
    @Column(nullable = false)
    private int houses = 0;

    /** Data dodania nieruchomosci — wymagana do filtrowania wg daty. */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private User owner;

    public Property() {
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

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getRent() {
        return rent;
    }

    public void setRent(BigDecimal rent) {
        this.rent = rent;
    }

    public String getColorGroup() {
        return colorGroup;
    }

    public void setColorGroup(String colorGroup) {
        this.colorGroup = colorGroup;
    }

    public int getHouses() {
        return houses;
    }

    public void setHouses(int houses) {
        this.houses = houses;
    }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public User getOwner() {
        return owner;
    }

    public void setOwner(User owner) {
        this.owner = owner;
    }
}
