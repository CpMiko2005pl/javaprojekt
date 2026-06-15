package pl.pb.monopoly.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.pb.monopoly.domain.BoardTile;

import java.util.List;
import java.util.Optional;

public interface BoardTileRepository extends JpaRepository<BoardTile, Long> {

    Optional<BoardTile> findByPosition(Integer position);

    List<BoardTile> findAllByOrderByPositionAsc();
}
