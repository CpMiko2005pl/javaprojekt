package pl.pb.monopoly.dto;

/** Karta Szansy wylosowana w turze - do animacji na froncie. */
public record ChanceCardDto(
        String title,
        String description,
        int moneyEffect
) {
}
