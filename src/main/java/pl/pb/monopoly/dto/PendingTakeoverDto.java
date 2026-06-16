package pl.pb.monopoly.dto;

public record PendingTakeoverDto(
        int position,
        String tileName,
        int price,
        Long buyerId,
        Long sellerId,
        String sellerName
) {}
