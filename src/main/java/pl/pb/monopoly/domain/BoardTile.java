package pl.pb.monopoly.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Pole planszy (kampus PB) jako encja slownikowa.
 * Wczesniej dane pol byly trzymane na sztywno w tablicach w GameService/GameEconomy
 * (nazwy, opisy, ceny, czynsze, typ). Tutaj sa przeniesione do tabeli board_tiles,
 * dzieki czemu plansze mozna edytowac bez przebudowy aplikacji.
 */
@Entity
@Table(name = "board_tiles")
public class BoardTile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Pozycja na planszy 0-39 (unikalna). */
    @NotNull
    @Column(nullable = false, unique = true)
    private Integer position;

    @NotBlank
    @Size(max = 80)
    @Column(nullable = false, length = 80)
    private String name;

    @Size(max = 255)
    @Column(length = 255)
    private String description;

    /** Cena zakupu w PLN; null dla pol nie bedacych nieruchomosciami. */
    private Integer price;

    /** Bazowy czynsz w PLN; null dla pol bez czynszu. */
    private Integer rent;

    /** Typ pola: START, PROPERTY, RESORT, UTILITY, CHANCE, COMMUNITY, TAX, JAIL, GO_TO_JAIL, FREE_PARKING. */
    @NotBlank
    @Size(max = 30)
    @Column(name = "tile_type", nullable = false, length = 30)
    private String tileType;

    @Size(max = 80)
    @Column(name = "img_slug", length = 80)
    private String imgSlug;

    public BoardTile() {
    }

    public BoardTile(Integer position, String name, String description, Integer price,
                     Integer rent, String tileType, String imgSlug) {
        this.position = position;
        this.name = name;
        this.description = description;
        this.price = price;
        this.rent = rent;
        this.tileType = tileType;
        this.imgSlug = imgSlug;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getPosition() {
        return position;
    }

    public void setPosition(Integer position) {
        this.position = position;
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

    public Integer getPrice() {
        return price;
    }

    public void setPrice(Integer price) {
        this.price = price;
    }

    public Integer getRent() {
        return rent;
    }

    public void setRent(Integer rent) {
        this.rent = rent;
    }

    public String getTileType() {
        return tileType;
    }

    public void setTileType(String tileType) {
        this.tileType = tileType;
    }

    public String getImgSlug() {
        return imgSlug;
    }

    public void setImgSlug(String imgSlug) {
        this.imgSlug = imgSlug;
    }
}
