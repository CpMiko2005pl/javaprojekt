package pl.pb.monopoly.dto;

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
        boolean isMe,
        boolean bot,
        List<Integer> ownedPositions,
        Map<Integer, Integer> propertyLevels
) {
}
