package pl.pb.monopoly.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.pb.monopoly.domain.GameLog;

import java.util.List;

public interface GameLogRepository extends JpaRepository<GameLog, Long> {

    List<GameLog> findByUserIdOrderByOccurredAtDesc(Long userId);
}
