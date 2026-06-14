package pl.pb.monopoly.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pl.pb.monopoly.domain.GameSession;
import pl.pb.monopoly.domain.GameStatus;
import pl.pb.monopoly.domain.User;
import pl.pb.monopoly.repository.MatchHistoryRepository;
import pl.pb.monopoly.repository.UserRepository;
import pl.pb.monopoly.service.FriendService;
import pl.pb.monopoly.service.GameService;
import pl.pb.monopoly.util.PublicUrlHelper;

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
    private final MatchHistoryRepository matchHistoryRepository;
    private final UserRepository userRepository;

    public GameController(GameService gameService, FriendService friendService,
                          MatchHistoryRepository matchHistoryRepository,
                          UserRepository userRepository) {
        this.gameService = gameService;
        this.friendService = friendService;
        this.matchHistoryRepository = matchHistoryRepository;
        this.userRepository = userRepository;
    }

    /** Hub matchmakingu lub aktywne lobby — zawsze pod /game. */
    @GetMapping
    public String lobby(Authentication auth, Model model, HttpServletRequest request) {
        String me = auth.getName();
        User user = userRepository.findByUsername(me).orElseThrow();
        List<GameSession> openSessions = gameService.myActiveSessions(me);
        model.addAttribute("activeSessions", openSessions);
        model.addAttribute("friends", friendService.friendsOf(me));
        model.addAttribute("serverUrl", PublicUrlHelper.publicBaseUrl(request));
        model.addAttribute("pendingInvites", gameService.pendingInvitesFor(me));
        model.addAttribute("matches", matchHistoryRepository.findByUserIdOrderByPlayedAtDesc(user.getId())
                .stream().limit(6).toList());

        GameSession waitingLobby = openSessions.stream()
                .filter(s -> s.getStatus() == GameStatus.WAITING)
                .findFirst()
                .orElse(null);
        GameSession activeGame = openSessions.stream()
                .filter(s -> s.getStatus() == GameStatus.ACTIVE)
                .findFirst()
                .orElse(null);

        model.addAttribute("inLobby", waitingLobby != null);
        model.addAttribute("activeGame", activeGame);
        model.addAttribute("inActiveGame", activeGame != null && waitingLobby == null);
        if (waitingLobby != null) {
            model.addAttribute("sessionId", waitingLobby.getId());
            model.addAttribute("sessionName", waitingLobby.getName());
            model.addAttribute("sessionCode", waitingLobby.getCode());
        }
        if (activeGame != null) {
            model.addAttribute("activeGameId", activeGame.getId());
            model.addAttribute("activeGameName", activeGame.getName());
        }
        return "game/lobby";
    }

    @PostMapping("/create")
    public String create(@RequestParam(required = false) String name,
                         @RequestParam(required = false) List<Long> friendIds,
                         Authentication auth, RedirectAttributes ra) {
        try {
            GameSession session = gameService.createGame(auth.getName(), name, friendIds);
            ra.addFlashAttribute("message", "Pokój utworzony! Kod: " + session.getCode());
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/game";
    }

    @PostMapping("/join")
    public String join(@RequestParam String code, Authentication auth, RedirectAttributes ra) {
        try {
            gameService.joinByCode(code, auth.getName());
            ra.addFlashAttribute("message", "Dołączyłeś do pokoju. Kliknij Gotowy!");
            return "redirect:/game";
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
            return "redirect:/game";
        }
    }

    @GetMapping("/{id}")
    public String game(@PathVariable Long id, Authentication auth, Model model) {
        GameSession session = gameService.getSession(id);
        String username = auth.getName();
        var me = session.getPlayers().stream()
                .filter(p -> p.getUser() != null && p.getUser().getUsername().equals(username))
                .findFirst();
        if (me.isEmpty() || me.get().isBankrupt()) {
            return "redirect:/game";
        }
        if (session.getStatus() == GameStatus.FINISHED) {
            return "redirect:/game";
        }
        if (session.getStatus() == GameStatus.WAITING) {
            return "redirect:/game";
        }
        model.addAttribute("sessionId", session.getId());
        model.addAttribute("sessionName", session.getName());
        model.addAttribute("sessionCode", session.getCode());
        model.addAttribute("myPlayerId", me.get().getId());
        return "game/board";
    }

    @PostMapping("/{id}/leave")
    public String leave(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        try {
            gameService.leaveGame(id, auth.getName());
            ra.addFlashAttribute("message", "Opusciles gre.");
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/game";
    }
}
