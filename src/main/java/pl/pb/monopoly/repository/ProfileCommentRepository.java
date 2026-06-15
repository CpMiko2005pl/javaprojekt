package pl.pb.monopoly.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.pb.monopoly.domain.ProfileComment;

import java.util.List;
import java.util.Optional;

public interface ProfileCommentRepository extends JpaRepository<ProfileComment, Long> {

    List<ProfileComment> findByTargetIdOrderByCreatedAtDesc(Long targetId);

    Optional<ProfileComment> findByAuthorIdAndTargetId(Long authorId, Long targetId);

    long countByTargetId(Long targetId);
}
