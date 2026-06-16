package pl.pb.monopoly.service;

import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class UserPresenceService {

    private final ConcurrentMap<String, String> sessionToUser = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Set<String>> userSessions = new ConcurrentHashMap<>();

    public void registerSession(String username, String sessionId) {
        if (username == null || username.isBlank() || sessionId == null) {
            return;
        }
        String prev = sessionToUser.putIfAbsent(sessionId, username);
        if (prev == null) {
            userSessions.computeIfAbsent(username, k -> ConcurrentHashMap.newKeySet()).add(sessionId);
        }
    }

    public void removeSession(String sessionId) {
        if (sessionId == null) {
            return;
        }
        String username = sessionToUser.remove(sessionId);
        if (username == null) {
            return;
        }
        Set<String> sessions = userSessions.get(username);
        if (sessions != null) {
            sessions.remove(sessionId);
            if (sessions.isEmpty()) {
                userSessions.remove(username, sessions);
            }
        }
    }

    public boolean isOnline(String username) {
        if (username == null || username.isBlank()) {
            return false;
        }
        Set<String> sessions = userSessions.get(username);
        return sessions != null && !sessions.isEmpty();
    }
}
