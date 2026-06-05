package pl.pb.monopoly.dto;

import java.util.Map;

/**
 * Gracz nie moze oplacic zobowiazania — musi uzbierac kwote (sprzedaz, pozyczka) lub zbankrutowac.
 */
public record PendingPaymentDto(
        Long debtorId,
        int amount,
        Long creditorId,
        String reason,
        /** Pozycja pola -> cena sprzedazy (50% ceny zakupu). */
        Map<Integer, Integer> sellPrices
) {
}
