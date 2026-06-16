package pl.pb.monopoly.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.pb.monopoly.domain.OwnedItem;

import java.util.List;

public interface OwnedItemRepository extends JpaRepository<OwnedItem, Long> {

    List<OwnedItem> findByUserIdOrderByObtainedAtDesc(Long userId);

    long countByUserId(Long userId);

    List<OwnedItem> findByUserIdAndEquipped(Long userId, boolean equipped);

    java.util.Optional<OwnedItem> findByUserIdAndItemSlug(Long userId, String itemSlug);

    void deleteByUserId(Long userId);
}
