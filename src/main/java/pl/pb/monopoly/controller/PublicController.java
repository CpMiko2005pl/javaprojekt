package pl.pb.monopoly.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import pl.pb.monopoly.domain.ProfileComment;
import pl.pb.monopoly.domain.Role;
import pl.pb.monopoly.domain.User;
import pl.pb.monopoly.dto.ProfileCommentDto;
import pl.pb.monopoly.repository.MatchHistoryRepository;
import pl.pb.monopoly.repository.ProfileCommentRepository;
import pl.pb.monopoly.repository.UserRepository;
import pl.pb.monopoly.service.UserPresenceService;

import java.util.List;

/**
 * Publiczne profile graczy — dostepne bez logowania (wymaganie: wyswietlenie z linku).
 * URL: /u/{username}
 */
@Controller
public class PublicController {

    private final UserRepository userRepository;
    private final MatchHistoryRepository matchHistoryRepository;
    private final UserPresenceService presenceService;
    private final ProfileCommentRepository commentRepository;

    public PublicController(UserRepository userRepository,
                            MatchHistoryRepository matchHistoryRepository,
                            UserPresenceService presenceService,
                            ProfileCommentRepository commentRepository) {
        this.userRepository = userRepository;
        this.matchHistoryRepository = matchHistoryRepository;
        this.presenceService = presenceService;
        this.commentRepository = commentRepository;
    }

    @GetMapping("/u/{username}")
    public String publicProfile(@PathVariable String username, Authentication auth, Model model) {
        var user = userRepository.findByUsername(username)
                .orElse(null);
        if (user == null) {
            model.addAttribute("error", "Nie znaleziono gracza: " + username);
            return "public/profile";
        }
        model.addAttribute("profileUser", user);
        model.addAttribute("profileOnline", presenceService.isOnline(user.getUsername()));
        model.addAttribute("stats", user.getStatistics());
        model.addAttribute("matches",
                matchHistoryRepository.findByUserIdOrderByPlayedAtDesc(user.getId()));

        // --- Komentarze pod profilem ---
        User viewer = currentUser(auth);
        Long viewerId = viewer != null ? viewer.getId() : null;
        boolean canModerate = viewer != null
                && (viewer.getRole() == Role.ADMIN || viewer.getRole() == Role.MODERATOR);

        List<ProfileCommentDto> comments = commentRepository
                .findByTargetIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(c -> {
                    boolean mine = viewerId != null && c.getAuthor().getId().equals(viewerId);
                    String label = displayLabel(c.getAuthor());
                    return new ProfileCommentDto(c.getId(), c.getAuthor().getUsername(), label,
                            c.getContent(), c.getCreatedAt(), c.getUpdatedAt(), mine, mine || canModerate);
                })
                .toList();
        model.addAttribute("comments", comments);

        boolean ownProfile = viewerId != null && viewerId.equals(user.getId());
        boolean alreadyCommented = viewerId != null
                && commentRepository.findByAuthorIdAndTargetId(viewerId, user.getId()).isPresent();
        model.addAttribute("loggedIn", viewer != null);
        model.addAttribute("canComment", viewer != null && !ownProfile && !alreadyCommented);
        model.addAttribute("ownProfile", ownProfile);
        return "public/profile";
    }

    private String displayLabel(User u) {
        if (u.getFirstName() != null && !u.getFirstName().isBlank()) {
            return u.getFirstName() + (u.getLastName() != null && !u.getLastName().isBlank()
                    ? " " + u.getLastName() : "");
        }
        return u.getUsername();
    }

    private User currentUser(Authentication auth) {
        if (auth == null || auth.getName() == null || "anonymousUser".equals(auth.getName())) {
            return null;
        }
        return userRepository.findByUsername(auth.getName()).orElse(null);
    }
}
