package pl.pb.monopoly.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import pl.pb.monopoly.repository.MatchHistoryRepository;
import pl.pb.monopoly.repository.UserRepository;

/**
 * Publiczne profile graczy — dostepne bez logowania (wymaganie: wyswietlenie z linku).
 * URL: /u/{username}
 */
@Controller
public class PublicController {

    private final UserRepository userRepository;
    private final MatchHistoryRepository matchHistoryRepository;

    public PublicController(UserRepository userRepository,
                            MatchHistoryRepository matchHistoryRepository) {
        this.userRepository = userRepository;
        this.matchHistoryRepository = matchHistoryRepository;
    }

    @GetMapping("/u/{username}")
    public String publicProfile(@PathVariable String username, Model model) {
        var user = userRepository.findByUsername(username)
                .orElse(null);
        if (user == null) {
            model.addAttribute("error", "Nie znaleziono gracza: " + username);
            return "public/profile";
        }
        model.addAttribute("profileUser", user);
        model.addAttribute("stats", user.getStatistics());
        model.addAttribute("matches",
                matchHistoryRepository.findByUserIdOrderByPlayedAtDesc(user.getId()));
        return "public/profile";
    }
}
