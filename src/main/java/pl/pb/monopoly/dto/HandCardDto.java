package pl.pb.monopoly.dto;

/** Karta w rece gracza — przekazywana TYLKO do wlasciciela kart. */
public record HandCardDto(
        String type,
        String label,
        String description,
        String iconClass
) {}
