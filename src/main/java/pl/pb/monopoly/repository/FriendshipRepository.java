package pl.pb.monopoly.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.pb.monopoly.domain.FriendStatus;
import pl.pb.monopoly.domain.Friendship;

import java.util.List;
import java.util.Optional;

public interface FriendshipRepository extends JpaRepository<Friendship, Long> {

    /** Zaakceptowane znajomosci, w ktorych dany uzytkownik jest nadawca lub odbiorca. */
    List<Friendship> findByStatusAndRequesterIdOrStatusAndAddresseeId(
            FriendStatus s1, Long requesterId, FriendStatus s2, Long addresseeId);

    /** Oczekujace zaproszenia skierowane do danego uzytkownika. */
    List<Friendship> findByAddresseeIdAndStatus(Long addresseeId, FriendStatus status);

    Optional<Friendship> findByRequesterIdAndAddresseeId(Long requesterId, Long addresseeId);

    boolean existsByRequesterIdAndAddresseeId(Long requesterId, Long addresseeId);
}
