package pl.pb.monopoly.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.pb.monopoly.domain.MonopolyCard;

public interface MonopolyCardRepository extends JpaRepository<MonopolyCard, Long> {
}
