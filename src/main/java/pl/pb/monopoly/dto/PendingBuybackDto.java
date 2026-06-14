package pl.pb.monopoly.dto;

import java.util.Map;

/** Odkup posesji przejetej karta — ofiara moze zaplacic 2x cene zakupu. */
public record PendingBuybackDto(
        int position,
        String tileName,
        int price,
        Long victimId,
        Long holderId,
        String holderName,
        Map<Integer, Integer> sellPrices
) {}
