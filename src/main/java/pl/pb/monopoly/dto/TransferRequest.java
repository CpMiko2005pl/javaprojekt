package pl.pb.monopoly.dto;

public record TransferRequest(Long toPlayerId, int amount) {
}
