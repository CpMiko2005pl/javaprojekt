package pl.pb.monopoly.dto;

/**
 * Pole, ktore obecnie czeka na decyzje "kup / pomin" przez aktywnego gracza.
 * Jezeli aktywny gracz pomija - pozostali gracze moga zlozyc oferte (licytacja).
 */
public record PendingPurchaseDto(
        int position,
        String tileName,
        int basePrice,
        Long deciderId,
        int minBid
) {
}
