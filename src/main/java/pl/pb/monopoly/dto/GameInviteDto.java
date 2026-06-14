package pl.pb.monopoly.dto;

public record GameInviteDto(
        Long id,
        Long sessionId,
        String sessionName,
        String roomCode,
        String inviterName,
        Long inviterId
) {
}
