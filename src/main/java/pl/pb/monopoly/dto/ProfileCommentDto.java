package pl.pb.monopoly.dto;

import java.time.LocalDateTime;

public record ProfileCommentDto(
        Long id,
        String authorUsername,
        String authorLabel,
        String content,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String reply,
        LocalDateTime replyAt,
        long likeCount,
        boolean likedByMe,
        boolean mine,
        boolean canEdit,
        boolean canDelete,
        boolean canReply
) {
}
