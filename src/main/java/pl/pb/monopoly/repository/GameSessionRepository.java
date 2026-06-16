package pl.pb.monopoly.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pl.pb.monopoly.domain.GameSession;

import java.util.List;
import java.util.Optional;

public interface GameSessionRepository extends JpaRepository<GameSession, Long> {

    Optional<GameSession> findByCode(String code);

    boolean existsByCode(String code);

    @Query("select distinct s from GameSession s join s.players p " +
           "where p.user.username = :username and s.status = pl.pb.monopoly.domain.GameStatus.ACTIVE " +
           "order by s.createdAt desc")
    List<GameSession> findActiveByUser(@Param("username") String username);

    @Query("select distinct s from GameSession s join s.players p " +
           "where p.user.username = :username and p.bankrupt = false " +
           "and s.status in (pl.pb.monopoly.domain.GameStatus.WAITING, pl.pb.monopoly.domain.GameStatus.ACTIVE) " +
           "order by s.createdAt desc")
    List<GameSession> findOpenByUser(@Param("username") String username);

    @Query("select s from GameSession s where s.status = pl.pb.monopoly.domain.GameStatus.ACTIVE order by s.createdAt desc")
    List<GameSession> findAllActive();
}
