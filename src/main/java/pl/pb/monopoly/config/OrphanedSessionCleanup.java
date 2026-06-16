package pl.pb.monopoly.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pl.pb.monopoly.domain.GameSession;
import pl.pb.monopoly.domain.GameStatus;
import pl.pb.monopoly.repository.GameSessionRepository;

import java.util.List;

@Component
public class OrphanedSessionCleanup {

    private static final Logger log = LoggerFactory.getLogger(OrphanedSessionCleanup.class);

    private final GameSessionRepository sessionRepository;

    public OrphanedSessionCleanup(GameSessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void closeOrphanedActiveSessions() {
        List<GameSession> orphans = sessionRepository.findAllActive();
        if (orphans.isEmpty()) return;
        for (GameSession s : orphans) {
            s.setStatus(GameStatus.FINISHED);
        }
        sessionRepository.saveAll(orphans);
        log.info("Oznaczono {} osieroconych sesji ACTIVE jako FINISHED (RAM pusty po restarcie).",
                orphans.size());
    }
}
