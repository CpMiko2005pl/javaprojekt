package pl.pb.monopoly.dto;

/** Znajomy na liscie (panel w stylu FACEIT). */
public record FriendDto(
        Long friendshipId,
        Long userId,
        String username,
        int level,
        int eloPoints
) {
}
