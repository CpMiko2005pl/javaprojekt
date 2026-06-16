package pl.pb.monopoly.dto;

public record GameInviteNotificationDto(
        String type,
        Long inviteId,
        Long sessionId,
        String sessionName,
        String roomCode,
        String inviterName,
        String message
) {
    public static GameInviteNotificationDto of(GameInviteDto invite) {
        return new GameInviteNotificationDto(
                "GAME_INVITE",
                invite.id(),
                invite.sessionId(),
                invite.sessionName(),
                invite.roomCode(),
                invite.inviterName(),
                invite.inviterName() + " zaprasza Cię do pokoju „" + invite.sessionName() + "”"
        );
    }
}
