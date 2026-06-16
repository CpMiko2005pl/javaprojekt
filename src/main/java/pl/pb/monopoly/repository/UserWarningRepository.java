package pl.pb.monopoly.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.pb.monopoly.domain.UserWarning;

import java.util.List;

public interface UserWarningRepository extends JpaRepository<UserWarning, Long> {

    List<UserWarning> findByUserIdOrderByCreatedAtDesc(Long userId);

    long countByUserId(Long userId);

    void deleteByUserId(Long userId);

    void deleteByModeratorId(Long moderatorId);
}
