package pl.pb.monopoly.service;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import pl.pb.monopoly.dto.GameStateDto;

@Service
public class GameSyncService {

    private final SimpMessagingTemplate messagingTemplate;

    public GameSyncService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void broadcastReaction(Long sessionId, Object payload) {
        messagingTemplate.convertAndSend("/topic/game/" + sessionId + "/reactions", payload);
    }
    public void broadcast(Long sessionId, GameStateDto state) {
        messagingTemplate.convertAndSend("/topic/game/" + sessionId, state);
    }
}
