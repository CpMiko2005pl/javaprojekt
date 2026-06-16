package pl.pb.monopoly.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pl.pb.monopoly.domain.FriendStatus;
import pl.pb.monopoly.domain.Friendship;

import java.util.List;
import java.util.Optional;

public interface FriendshipRepository extends JpaRepository<Friendship, Long> {

    List<Friendship> findByStatusAndRequesterIdOrStatusAndAddresseeId(
            FriendStatus s1, Long requesterId, FriendStatus s2, Long addresseeId);

    List<Friendship> findByAddresseeIdAndStatus(Long addresseeId, FriendStatus status);

    Optional<Friendship> findByRequesterIdAndAddresseeId(Long requesterId, Long addresseeId);

    boolean existsByRequesterIdAndAddresseeId(Long requesterId, Long addresseeId);

    @Modifying
    @Query("DELETE FROM Friendship f WHERE f.requester.id = :userId OR f.addressee.id = :userId")
    void deleteAllForUser(@Param("userId") Long userId);
}
