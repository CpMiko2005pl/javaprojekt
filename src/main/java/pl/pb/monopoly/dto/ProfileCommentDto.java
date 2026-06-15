package pl.pb.monopoly.dto;

import java.time.LocalDateTime;

/**
 * Komentarz pod profilem przygotowany do wyswietlenia (bez leniwych encji).
 */
public record ProfileCommentDto(
        Long id,
        String authorUsername,
        String authorLabel,
        String content,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        boolean mine,
        boolean canDelete
) {
}
