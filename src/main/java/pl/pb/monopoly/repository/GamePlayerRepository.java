package pl.pb.monopoly.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.pb.monopoly.domain.GamePlayer;

public interface GamePlayerRepository extends JpaRepository<GamePlayer, Long> {
}
