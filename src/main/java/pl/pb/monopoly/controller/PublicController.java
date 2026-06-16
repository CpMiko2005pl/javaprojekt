package pl.pb.monopoly.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import pl.pb.monopoly.domain.User;
import pl.pb.monopoly.repository.MatchHistoryRepository;
import pl.pb.monopoly.repository.UserRepository;
import pl.pb.monopoly.service.ProfileCommentService;
import pl.pb.monopoly.service.UserPresenceService;

@Controller
public class PublicController {

    private final UserRepository userRepository;
    private final MatchHistoryRepository matchHistoryRepository;
    private final UserPresenceService presenceService;
    private final ProfileCommentService profileCommentService;

    public PublicController(UserRepository userRepository,
                            MatchHistoryRepository matchHistoryRepository,
                            UserPresenceService presenceService,
                            ProfileCommentService profileCommentService) {
        this.userRepository = userRepository;
        this.matchHistoryRepository = matchHistoryRepository;
        this.presenceService = presenceService;
        this.profileCommentService = profileCommentService;
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

        User viewer = currentUser(auth);
        model.addAttribute("comments", profileCommentService.commentsFor(user, viewer));
        model.addAttribute("commentTargetUsername", user.getUsername());
        model.addAttribute("commentFrom", "/u/" + user.getUsername());
        boolean ownProfile = viewer != null && viewer.getId().equals(user.getId());
        boolean alreadyCommented = viewer != null
                && profileCommentService.hasCommented(viewer.getId(), user.getId());
        model.addAttribute("loggedIn", viewer != null);
        model.addAttribute("canComment", viewer != null && !ownProfile && !alreadyCommented);
        model.addAttribute("ownProfile", ownProfile);
        return "public/profile";
    }

    private User currentUser(Authentication auth) {
        if (auth == null || auth.getName() == null || "anonymousUser".equals(auth.getName())) {
            return null;
        }
        return userRepository.findByUsername(auth.getName()).orElse(null);
    }
}
