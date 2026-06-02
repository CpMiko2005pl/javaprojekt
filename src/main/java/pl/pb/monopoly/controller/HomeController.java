package pl.pb.monopoly.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import pl.pb.monopoly.domain.User;
import pl.pb.monopoly.repository.MatchHistoryRepository;
import pl.pb.monopoly.repository.UserRepository;
import pl.pb.monopoly.service.GameService;
import pl.pb.monopoly.service.UserService;
import pl.pb.monopoly.service.WheelService;

/**
 * Strony ogolne: strona powitalna oraz panel gracza w stylu FACEIT
 * (poziom, ELO, statystyki, historia meczow). Saldo NIE jest tu pokazywane -
 * "siano" istnieje tylko podczas rozgrywki.
 */
@Controller
public class HomeController {

    private final UserRepository userRepository;
    private final MatchHistoryRepository matchHistoryRepository;
    private final GameService gameService;
    private final WheelService wheelService;
    private final UserService userService;

    public HomeController(UserRepository userRepository,
                          MatchHistoryRepository matchHistoryRepository,
                          GameService gameService,
                          WheelService wheelService,
                          UserService userService) {
        this.userRepository = userRepository;
        this.matchHistoryRepository = matchHistoryRepository;
        this.gameService = gameService;
        this.wheelService = wheelService;
        this.userService = userService;
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("ranking", userService.topPlayers());
        return "index";
    }

    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication, Model model) {
        User user = userRepository.findByUsername(authentication.getName()).orElseThrow();
        model.addAttribute("user", user);
        model.addAttribute("stats", user.getStatistics());
        model.addAttribute("matches", matchHistoryRepository.findByUserIdOrderByPlayedAtDesc(user.getId()));
        model.addAttribute("activeSessions", gameService.myActiveSessions(user.getUsername()));
        model.addAttribute("canSpin", wheelService.canSpinToday(user.getUsername()));
        return "dashboard";
    }
}
