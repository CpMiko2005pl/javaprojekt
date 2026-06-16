package pl.pb.monopoly.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pl.pb.monopoly.domain.GameInvite;
import pl.pb.monopoly.domain.GameInviteStatus;

import java.util.List;
import java.util.Optional;

public interface GameInviteRepository extends JpaRepository<GameInvite, Long> {

    List<GameInvite> findByInviteeIdAndStatusOrderByCreatedAtDesc(Long inviteeId, GameInviteStatus status);

    Optional<GameInvite> findBySessionIdAndInviteeIdAndStatus(Long sessionId, Long inviteeId, GameInviteStatus status);

    boolean existsBySessionIdAndInviteeIdAndStatus(Long sessionId, Long inviteeId, GameInviteStatus status);

    @Modifying
    @Query("DELETE FROM GameInvite gi WHERE gi.inviter.id = :userId OR gi.invitee.id = :userId")
    void deleteAllForUser(@Param("userId") Long userId);
}
