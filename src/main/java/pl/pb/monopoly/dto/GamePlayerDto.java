package pl.pb.monopoly.dto;

/** Stan jednego gracza w sesji - przekazywany do planszy na canvasie. */
public record GamePlayerDto(
        Long id,
        String name,
        int cash,
        int position,
        String color,
        boolean bankrupt,
        boolean isMe
) {
}
