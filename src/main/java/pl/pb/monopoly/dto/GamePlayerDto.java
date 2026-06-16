package pl.pb.monopoly.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

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

        String pawnModel
) {
}
