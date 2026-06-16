package pl.pb.monopoly.dto;

public record FriendDto(
        Long friendshipId,
        Long userId,
        String username,
        int level,
        int eloPoints,
        boolean online
) {
}
