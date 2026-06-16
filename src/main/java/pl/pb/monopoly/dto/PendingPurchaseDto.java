package pl.pb.monopoly.dto;

public record PendingPurchaseDto(
        int position,
        String tileName,
        int basePrice,
        Long deciderId,
        int minBid,
        int baseRent
) {
}
