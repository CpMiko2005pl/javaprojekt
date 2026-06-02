package pl.pb.monopoly.dto;

/** Zadanie transferu "siana" do innego gracza w sesji. */
public record TransferRequest(Long toPlayerId, int amount) {
}
