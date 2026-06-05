package pl.pb.monopoly.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import pl.pb.monopoly.domain.OwnedItem;
import pl.pb.monopoly.domain.User;
import pl.pb.monopoly.repository.MatchHistoryRepository;
import pl.pb.monopoly.repository.UserRepository;
import pl.pb.monopoly.service.FriendService;
import pl.pb.monopoly.service.GameService;
import pl.pb.monopoly.service.LootboxService;
import pl.pb.monopoly.service.LootboxService.LootboxItem;
import pl.pb.monopoly.service.UserService;
import pl.pb.monopoly.service.WheelService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Strony ogolne: strona powitalna oraz panel gracza w stylu FACEIT
 * (poziom, ELO, statystyki, historia meczow, znajomi, szybki dostep do funkcji,
 * lootbox + ekwipunek).
 */
@Controller
public class HomeController {

    private final UserRepository userRepository;
    private final MatchHistoryRepository matchHistoryRepository;
    private final GameService gameService;
    private final WheelService wheelService;
    private final UserService userService;
    private final FriendService friendService;
    private final LootboxService lootboxService;

    public HomeController(UserRepository userRepository,
                          MatchHistoryRepository matchHistoryRepository,
                          GameService gameService,
                          WheelService wheelService,
                          UserService userService,
                          FriendService friendService,
                          LootboxService lootboxService) {
        this.userRepository = userRepository;
        this.matchHistoryRepository = matchHistoryRepository;
        this.gameService = gameService;
        this.wheelService = wheelService;
        this.userService = userService;
        this.friendService = friendService;
        this.lootboxService = lootboxService;
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("ranking", userService.topPlayers());
        return "index";
    }

    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication,
                            @RequestParam(value = "q", required = false) String query,
                            Model model) {
        // Moderatorzy i adminowie maja wlasny panel - nie profil gracza
        boolean isMod = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_MODERATOR"));
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (isMod && !isAdmin) {
            return "redirect:/moderator";
        }

        // Codzienna darmowa skrzynka
        lootboxService.grantDailyIfNeeded(authentication.getName());

        User user = userRepository.findByUsername(authentication.getName()).orElseThrow();
        model.addAttribute("user", user);
        model.addAttribute("stats", user.getStatistics());
        model.addAttribute("matches", matchHistoryRepository.findByUserIdOrderByPlayedAtDesc(user.getId()));
        model.addAttribute("activeSessions", gameService.myActiveSessions(user.getUsername()));
        model.addAttribute("canSpin", wheelService.canSpinToday(user.getUsername()));
        model.addAttribute("friends", friendService.friendsOf(user.getUsername()));
        model.addAttribute("requests", friendService.pendingFor(user.getUsername()));
        model.addAttribute("query", query);
        model.addAttribute("results", friendService.search(query, user.getUsername()));
        model.addAttribute("availableBoxes", user.getStatistics() != null
                ? user.getStatistics().getAvailableLootboxes() : 0);
        model.addAttribute("inventory", buildInventory(user));
        return "dashboard";
    }

    private List<Map<String, Object>> buildInventory(User user) {
        List<OwnedItem> items = lootboxService.inventoryOf(user.getId());
        Map<String, LootboxItem> bySlug = LootboxService.bySlug();
        return items.stream().map(o -> {
            LootboxItem t = bySlug.get(o.getItemSlug());
            Map<String, Object> m = new HashMap<>();
            m.put("ownedId", o.getId());
            m.put("slug", o.getItemSlug());
            m.put("obtainedAt", o.getObtainedAt());
            m.put("equipped", o.isEquipped());
            if (t != null) {
                m.put("name", t.name());
                m.put("rarity", t.rarity().name());
                m.put("rarityLabel", t.rarity().label);
                m.put("rarityColor", t.rarity().color);
                m.put("category", t.category());
                m.put("iconClass", t.iconClass());
                m.put("description", t.description());
            } else {
                m.put("name", o.getItemSlug());
                m.put("rarity", "COMMON");
                m.put("rarityLabel", "Common");
                m.put("rarityColor", "#9ca3af");
                m.put("iconClass", "fa-solid fa-cube");
                m.put("category", "Przedmiot");
            }
            return m;
        }).collect(Collectors.toList());
    }
}
