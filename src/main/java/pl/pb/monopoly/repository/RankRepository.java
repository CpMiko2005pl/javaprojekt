package pl.pb.monopoly.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.pb.monopoly.domain.Rank;

import java.util.Optional;

public interface RankRepository extends JpaRepository<Rank, Long> {

    /** Ranga dla podanej liczby punktow ELO (prog dolny/gorny wlacznie). */
    Optional<Rank> findFirstByEloFromLessThanEqualAndEloToGreaterThanEqual(int eloLow, int eloHigh);
}
