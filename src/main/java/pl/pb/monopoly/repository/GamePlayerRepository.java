package pl.pb.monopoly.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.pb.monopoly.domain.GamePlayer;

import java.util.List;

public interface GamePlayerRepository extends JpaRepository<GamePlayer, Long> {

    List<GamePlayer> findByUser_Id(Long userId);
}
