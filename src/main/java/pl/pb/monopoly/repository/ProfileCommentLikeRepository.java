package pl.pb.monopoly.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.pb.monopoly.domain.ProfileCommentLike;

import java.util.List;
import java.util.Optional;

public interface ProfileCommentLikeRepository extends JpaRepository<ProfileCommentLike, Long> {

    long countByCommentId(Long commentId);

    Optional<ProfileCommentLike> findByCommentIdAndUserId(Long commentId, Long userId);

    List<ProfileCommentLike> findByCommentIdIn(List<Long> commentIds);

    void deleteByCommentId(Long commentId);

    void deleteByCommentIdIn(java.util.Collection<Long> commentIds);

    void deleteByUserId(Long userId);
}
