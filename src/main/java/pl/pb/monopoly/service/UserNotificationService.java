package pl.pb.monopoly.service;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import pl.pb.monopoly.dto.GameInviteNotificationDto;

/** Powiadomienia per uzytkownik (zaproszenia do gry itp.). */
@Service
public class UserNotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    public UserNotificationService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void sendGameInvite(String inviteeUsername, GameInviteNotificationDto notification) {
        messagingTemplate.convertAndSend("/topic/user/" + inviteeUsername, notification);
    }
}
