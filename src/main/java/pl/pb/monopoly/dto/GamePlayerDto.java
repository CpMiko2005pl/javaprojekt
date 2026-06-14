package pl.pb.monopoly.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/** Stan jednego gracza w sesji - przekazywany do planszy na canvasie. */
public record GamePlayerDto(
        Long id,
        String name,
        int cash,
        int position,
        String color,
        boolean bankrupt,
        @JsonProperty("isMe") boolean isMe,
        boolean bot,
        List<Integer> ownedPositions,
        Map<Integer, Integer> propertyLevels,
        boolean ready,
        boolean leader,
        /** Sciezka GLB zalozonego pionka 3D (np. /models/PawnSteve.glb) lub null = domyslny. */
        String pawnModel
) {
}
