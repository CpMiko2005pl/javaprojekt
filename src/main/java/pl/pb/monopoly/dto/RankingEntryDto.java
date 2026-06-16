package pl.pb.monopoly.dto;

public record RankingEntryDto(
        int pozycja,
        String login,
        int poziom,
        int eloPoints,
        int wygranePartie
) {
}
