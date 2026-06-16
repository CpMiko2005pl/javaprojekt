package pl.pb.monopoly.dto;

import java.util.List;
import java.util.Map;

public record GameStateDto(
        Long sessionId,
        String code,
        String name,
        String status,
        List<GamePlayerDto> players,
        Long currentTurnPlayerId,
        Integer dice1,
        Integer dice2,
        String message,
        Long movedPlayerId,
        Integer fromPosition,
        Integer toPosition,
        boolean myTurn,
        List<String> tileNames,
        List<String> tileEffects,
        PendingPurchaseDto pendingPurchase,
        PendingPaymentDto pendingPayment,
        Map<Integer, Long> ownership,
        List<Integer> tilePrices,
        ChanceCardDto chanceCard,
        Long winnerId,
        String winnerName,
        List<HandCardDto> myHandCards,
        PendingUpgradeDto pendingUpgrade,
        PendingBuybackDto pendingBuyback,
        Long leaderId,
        boolean canRollAgain,
        List<PropertyCardDto> myPropertyCards,
        Long secondsLeft,
        PendingTakeoverDto pendingTakeover
) {
}
