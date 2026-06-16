package pl.pb.monopoly.dto;

import java.util.Map;

public record PendingPaymentDto(
        Long debtorId,
        int amount,
        Long creditorId,
        String reason,

        Map<Integer, Integer> sellPrices
) {
}
