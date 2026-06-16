package pl.pb.monopoly.dto;

/**
 * Wykup cudzej dzialki (Business Tour): gracz, ktory wyladowal na polu rywala
 * i oplacil czynsz, moze odkupic to pole od wlasciciela za podana cene.
 */
public record PendingTakeoverDto(
        int position,
        String tileName,
        int price,
        Long buyerId,
        Long sellerId,
        String sellerName
) {}
