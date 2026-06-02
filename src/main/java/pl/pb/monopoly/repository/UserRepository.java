package pl.pb.monopoly.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import pl.pb.monopoly.domain.User;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    /** Wyszukiwanie graczy po fragmencie loginu (dodawanie znajomych). */
    List<User> findTop10ByUsernameContainingIgnoreCase(String fragment);

    /** Ranking najlepszych graczy wg punktow ELO (usluga REST), malejaco. */
    @Query("select u from User u join u.statistics s order by s.eloPoints desc, s.gamesWon desc")
    List<User> findTopPlayers(Pageable pageable);
}
