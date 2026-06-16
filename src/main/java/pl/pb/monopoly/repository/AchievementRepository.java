package pl.pb.monopoly.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.pb.monopoly.domain.Achievement;
import pl.pb.monopoly.domain.AchievementType;

import java.util.List;

public interface AchievementRepository extends JpaRepository<Achievement, Long> {

    boolean existsByUserIdAndCode(Long userId, AchievementType code);

    List<Achievement> findByUserIdOrderByUnlockedAtDesc(Long userId);
}
