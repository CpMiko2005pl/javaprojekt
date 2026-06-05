package pl.pb.monopoly.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.pb.monopoly.domain.OwnedItem;

import java.util.List;

public interface OwnedItemRepository extends JpaRepository<OwnedItem, Long> {

    List<OwnedItem> findByUserIdOrderByObtainedAtDesc(Long userId);

    long countByUserId(Long userId);
}
