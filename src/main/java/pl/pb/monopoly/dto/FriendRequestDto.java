package pl.pb.monopoly.dto;

public record FriendRequestDto(
        Long friendshipId,
        Long fromUserId,
        String fromUsername,
        int level
) {
}
