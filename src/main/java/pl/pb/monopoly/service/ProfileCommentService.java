package pl.pb.monopoly.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.pb.monopoly.domain.ProfileComment;
import pl.pb.monopoly.domain.Role;
import pl.pb.monopoly.domain.User;
import pl.pb.monopoly.dto.ProfileCommentDto;
import pl.pb.monopoly.repository.ProfileCommentLikeRepository;
import pl.pb.monopoly.repository.ProfileCommentRepository;

import java.util.List;

@Service
public class ProfileCommentService {

    private final ProfileCommentRepository commentRepository;
    private final ProfileCommentLikeRepository likeRepository;

    public ProfileCommentService(ProfileCommentRepository commentRepository,
                                 ProfileCommentLikeRepository likeRepository) {
        this.commentRepository = commentRepository;
        this.likeRepository = likeRepository;
    }

    @Transactional(readOnly = true)
    public List<ProfileCommentDto> commentsFor(User target, User viewer) {
        Long viewerId = viewer != null ? viewer.getId() : null;
        boolean isOwner = viewerId != null && viewerId.equals(target.getId());
        boolean canModerate = viewer != null
                && (viewer.getRole() == Role.ADMIN || viewer.getRole() == Role.MODERATOR);

        return commentRepository.findByTargetIdOrderByCreatedAtDesc(target.getId()).stream()
                .map(c -> toDto(c, viewerId, isOwner, canModerate))
                .toList();
    }

    private ProfileCommentDto toDto(ProfileComment c, Long viewerId, boolean isOwner, boolean canModerate) {
        boolean mine = viewerId != null && c.getAuthor().getId().equals(viewerId);
        boolean likedByMe = viewerId != null
                && likeRepository.findByCommentIdAndUserId(c.getId(), viewerId).isPresent();
        long likes = likeRepository.countByCommentId(c.getId());
        String label = displayLabel(c.getAuthor());
        return new ProfileCommentDto(
                c.getId(),
                c.getAuthor().getUsername(),
                label,
                c.getContent(),
                c.getCreatedAt(),
                c.getUpdatedAt(),
                c.getReply(),
                c.getReplyAt(),
                likes,
                likedByMe,
                mine,
                mine,
                mine || isOwner || canModerate,
                isOwner
        );
    }

    public boolean hasCommented(Long authorId, Long targetId) {
        return commentRepository.findByAuthorIdAndTargetId(authorId, targetId).isPresent();
    }

    private static String displayLabel(User u) {
        if (u.getFirstName() != null && !u.getFirstName().isBlank()) {
            return u.getFirstName() + (u.getLastName() != null && !u.getLastName().isBlank()
                    ? " " + u.getLastName() : "");
        }
        return u.getUsername();
    }
}
