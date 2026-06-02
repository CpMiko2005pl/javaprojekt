package pl.pb.monopoly.dto;

/** Oczekujace zaproszenie do znajomych. */
public record FriendRequestDto(
        Long friendshipId,
        Long fromUserId,
        String fromUsername,
        int level
) {
}
