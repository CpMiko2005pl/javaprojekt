package pl.pb.monopoly.dto;

import java.util.Map;

public record PendingBuybackDto(
        int position,
        String tileName,
        int price,
        Long victimId,
        Long holderId,
        String holderName,
        Map<Integer, Integer> sellPrices
) {}
