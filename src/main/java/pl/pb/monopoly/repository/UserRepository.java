package pl.pb.monopoly.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pl.pb.monopoly.domain.Role;
import pl.pb.monopoly.domain.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    @Query("select u from User u where u.role = COALESCE(:role, u.role) " +
           "and u.createdAt >= COALESCE(:dateFrom, u.createdAt)")
    List<User> findForAdmin(@Param("role") Role role,
                            @Param("dateFrom") LocalDateTime dateFrom,
                            Sort sort);

    List<User> findTop10ByUsernameContainingIgnoreCase(String fragment);

    @Query("select u from User u join u.statistics s order by s.eloPoints desc, s.gamesWon desc")
    List<User> findTopPlayers(Pageable pageable);
}
