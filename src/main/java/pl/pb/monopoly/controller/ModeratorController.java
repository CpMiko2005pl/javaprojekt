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
import pl.pb.monopoly.domain.GameStatus;
import pl.pb.monopoly.domain.User;
import pl.pb.monopoly.repository.GameSessionRepository;
import pl.pb.monopoly.repository.MatchHistoryRepository;
import pl.pb.monopoly.repository.UserRepository;
import pl.pb.monopoly.service.ModerationService;
import pl.pb.monopoly.service.UserService;

import java.util.HashMap;
import java.util.Map;

/**
 * Panel moderatora (ROLE_MODERATOR i ROLE_ADMIN).
 * Moderator widzi aktywne gry i uzytkownikow, moze zakonczyc sesje,
 * weryfikowac konta, zawieszac graczy i wysylac ostrzezenia.
 */
@Controller
@RequestMapping("/moderator")
public class ModeratorController {

    private final GameSessionRepository gameSessionRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final MatchHistoryRepository matchHistoryRepository;
    private final ModerationService moderationService;

    public ModeratorController(GameSessionRepository gameSessionRepository,
                               UserRepository userRepository,
                               UserService userService,
                               MatchHistoryRepository matchHistoryRepository,
                               ModerationService moderationService) {
        this.gameSessionRepository = gameSessionRepository;
        this.userRepository = userRepository;
        this.userService = userService;
        this.matchHistoryRepository = matchHistoryRepository;
        this.moderationService = moderationService;
    }

    @GetMapping
    public String panel(Authentication auth, Model model) {
        model.addAttribute("activeSessions", gameSessionRepository.findAllActive());
        var users = userService.findAll();
        model.addAttribute("users", users);
        Map<Long, Long> warningCounts = new HashMap<>();
        for (User u : users) {
            warningCounts.put(u.getId(), moderationService.warningCount(u.getId()));
        }
        model.addAttribute("warningCounts", warningCounts);
        model.addAttribute("totalMatches", matchHistoryRepository.count());
        model.addAttribute("currentUser", auth.getName());
        return "moderator/panel";
    }

    @GetMapping("/users/{id}/warnings")
    public String userWarnings(@PathVariable Long id, Model model) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Nie znaleziono uzytkownika."));
        model.addAttribute("targetUser", user);
        model.addAttribute("warnings", moderationService.warningsForUser(id));
        return "moderator/warnings";
    }

    @PostMapping("/users/{id}/verify")
    public String verify(@PathVariable Long id,
                         @RequestParam(defaultValue = "true") boolean verified,
                         RedirectAttributes ra) {
        userService.setVerified(id, verified);
        ra.addFlashAttribute("message", verified ? "Konto zweryfikowane." : "Cofnieto weryfikacje.");
        return "redirect:/moderator";
    }

    @PostMapping("/users/{id}/suspend")
    public String suspend(@PathVariable Long id,
                          @RequestParam String reason,
                          @RequestParam(defaultValue = "0") int days,
                          Authentication auth,
                          RedirectAttributes ra) {
        try {
            moderationService.suspendUser(id, auth.getName(), reason, days > 0 ? days : null);
            ra.addFlashAttribute("message", "Uzytkownik zawieszony — nie moze grac.");
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/moderator";
    }

    @PostMapping("/users/{id}/unsuspend")
    public String unsuspend(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        try {
            moderationService.unsuspendUser(id, auth.getName());
            ra.addFlashAttribute("message", "Zawieszenie zniesione.");
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/moderator";
    }

    @PostMapping("/users/{id}/warn")
    public String warn(@PathVariable Long id,
                       @RequestParam String message,
                       Authentication auth,
                       RedirectAttributes ra) {
        try {
            moderationService.issueWarning(id, auth.getName(), message);
            ra.addFlashAttribute("message", "Wyslano ostrzezenie.");
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
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
