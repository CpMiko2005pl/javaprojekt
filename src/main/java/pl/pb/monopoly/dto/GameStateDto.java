package pl.pb.monopoly.dto;

import java.util.List;

/**
 * Pelny stan rozgrywki — REST, WebSocket i animacje na planszy 3D.
 */
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
        List<String> tileEffects
) {
}
