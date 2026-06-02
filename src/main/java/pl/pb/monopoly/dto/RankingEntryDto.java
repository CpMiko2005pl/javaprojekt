package pl.pb.monopoly.dto;

/**
 * Pojedynczy wpis rankingu najlepszych graczy zwracany przez usluge REST
 * (GET /api/ranking-najlepszych). Ranking oparty o punkty ELO (jak FACEIT).
 */
public record RankingEntryDto(
        int pozycja,
        String login,
        int poziom,
        int eloPoints,
        int wygranePartie
) {
}
