package pl.pb.monopoly.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pl.pb.monopoly.domain.GameSession;
import pl.pb.monopoly.service.FriendService;
import pl.pb.monopoly.service.GameService;

import java.util.List;

/**
 * Lobby rozgrywki (tworzenie pokoju, zapraszanie znajomych, dolaczanie po kodzie)
 * oraz strona planszy (canvas).
 */
@Controller
@RequestMapping("/game")
public class GameController {

    private final GameService gameService;
    private final FriendService friendService;

    public GameController(GameService gameService, FriendService friendService) {
        this.gameService = gameService;
        this.friendService = friendService;
    }

    @GetMapping
    public String lobby(Authentication auth, Model model) {
        String me = auth.getName();
        model.addAttribute("activeSessions", gameService.myActiveSessions(me));
        model.addAttribute("friends", friendService.friendsOf(me));
        return "game/lobby";
    }

    @PostMapping("/create")
    public String create(@RequestParam(required = false) String name,
                         @RequestParam(required = false) List<Long> friendIds,
                         @RequestParam(defaultValue = "1") int bots,
                         Authentication auth, RedirectAttributes ra) {
        GameSession session = gameService.createGame(auth.getName(), name, friendIds, bots);
        ra.addFlashAttribute("message", "Utworzono pokoj. Kod: " + session.getCode());
        return "redirect:/game/" + session.getId();
    }

    @PostMapping("/join")
    public String join(@RequestParam String code, Authentication auth, RedirectAttributes ra) {
        try {
            GameSession session = gameService.joinByCode(code, auth.getName());
            return "redirect:/game/" + session.getId();
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
            return "redirect:/game";
        }
    }

    @GetMapping("/{id}")
    public String board(@PathVariable Long id, Model model) {
        GameSession session = gameService.getSession(id);
        model.addAttribute("sessionId", session.getId());
        model.addAttribute("sessionName", session.getName());
        model.addAttribute("sessionCode", session.getCode());
        return "game/board";
    }
}
