package pl.pb.monopoly.service;

import org.springframework.stereotype.Component;
import pl.pb.monopoly.domain.GameSession;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Component
public class ActiveGameStore {

    private final Map<Long, GameSession> sessions = new ConcurrentHashMap<>();
    private final Map<Long, ReentrantLock> locks = new ConcurrentHashMap<>();

    public boolean contains(Long sessionId) {
        return sessionId != null && sessions.containsKey(sessionId);
    }

    public GameSession get(Long sessionId) {
        return sessionId == null ? null : sessions.get(sessionId);
    }

    public void put(GameSession session) {
        if (session != null && session.getId() != null) {
            sessions.put(session.getId(), session);
        }
    }

    public void remove(Long sessionId) {
        if (sessionId == null) return;
        sessions.remove(sessionId);

        locks.remove(sessionId);
    }

    public Collection<GameSession> activeSessions() {
        return sessions.values();
    }

    public int size() {
        return sessions.size();
    }

    public ReentrantLock lockFor(Long sessionId) {
        return locks.computeIfAbsent(sessionId, k -> new ReentrantLock());
    }
}
