package pl.pb.monopoly.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.pb.monopoly.domain.BoardCard;

import java.util.List;

public interface BoardCardRepository extends JpaRepository<BoardCard, Long> {

    List<BoardCard> findByCardType(String cardType);
}
