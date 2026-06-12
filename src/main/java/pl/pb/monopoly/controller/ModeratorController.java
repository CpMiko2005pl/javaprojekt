package pl.pb.monopoly.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pl.pb.monopoly.domain.GameSession;
import pl.pb.monopoly.domain.GameStatus;
import pl.pb.monopoly.repository.GameSessionRepository;
import pl.pb.monopoly.repository.MatchHistoryRepository;
import pl.pb.monopoly.repository.UserRepository;
import pl.pb.monopoly.service.UserService;

/**
 * Panel moderatora (ROLE_MODERATOR i ROLE_ADMIN).
 * Moderator widzi aktywne gry i uzytkownikow, moze zakonczyc sesje.
 * Nie ma wlasnego profilu — jest przekierowywany tu z /dashboard.
 */
@Controller
@RequestMapping("/moderator")
public class ModeratorController {

    private final GameSessionRepository gameSessionRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final MatchHistoryRepository matchHistoryRepository;

    public ModeratorController(GameSessionRepository gameSessionRepository,
                               UserRepository userRepository,
                               UserService userService,
                               MatchHistoryRepository matchHistoryRepository) {
        this.gameSessionRepository = gameSessionRepository;
        this.userRepository = userRepository;
        this.userService = userService;
        this.matchHistoryRepository = matchHistoryRepository;
    }

    @GetMapping
    public String panel(Authentication auth, Model model) {
        model.addAttribute("activeSessions", gameSessionRepository.findAllActive());
        model.addAttribute("users", userService.findAll());
        model.addAttribute("totalMatches", matchHistoryRepository.count());
        model.addAttribute("currentUser", auth.getName());
        return "moderator/panel";
    }

    /** Moderator moze weryfikowac konta graczy. */
    @PostMapping("/users/{id}/verify")
    public String verify(@PathVariable Long id,
                         @RequestParam(defaultValue = "true") boolean verified,
                         RedirectAttributes ra) {
        userService.setVerified(id, verified);
        ra.addFlashAttribute("message", verified ? "Konto zweryfikowane." : "Cofnieto weryfikacje.");
        return "redirect:/moderator";
    }

    @PostMapping("/sessions/{id}/end")
    public String endSession(@PathVariable Long id, RedirectAttributes ra) {
        gameSessionRepository.findById(id).ifPresent(s -> {
            s.setStatus(GameStatus.FINISHED);
            gameSessionRepository.save(s);
        });
        ra.addFlashAttribute("message", "Sesja zakonczona przez moderatora.");
        return "redirect:/moderator";
    }
}
