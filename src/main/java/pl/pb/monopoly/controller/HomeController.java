package pl.pb.monopoly.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpServletRequest;

import pl.pb.monopoly.domain.OwnedItem;
import pl.pb.monopoly.domain.GameStatus;
import pl.pb.monopoly.domain.User;
import pl.pb.monopoly.repository.MatchHistoryRepository;
import pl.pb.monopoly.repository.OwnedItemRepository;
import pl.pb.monopoly.repository.UserRepository;
import pl.pb.monopoly.service.FriendService;
import pl.pb.monopoly.service.GameService;
import pl.pb.monopoly.service.ModerationService;
import pl.pb.monopoly.service.AchievementService;
import pl.pb.monopoly.service.ProfileCommentService;
import pl.pb.monopoly.service.LootboxService;
import pl.pb.monopoly.service.LootboxService.LootboxItem;
import pl.pb.monopoly.service.UserService;
import pl.pb.monopoly.service.WheelService;
import pl.pb.monopoly.util.PublicUrlHelper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class HomeController {

    private final UserRepository userRepository;
    private final MatchHistoryRepository matchHistoryRepository;
    private final GameService gameService;
    private final WheelService wheelService;
    private final UserService userService;
    private final FriendService friendService;
    private final LootboxService lootboxService;
    private final OwnedItemRepository ownedItemRepository;
    private final Environment environment;
    private final ModerationService moderationService;
    private final ProfileCommentService profileCommentService;
    private final AchievementService achievementService;

    @Value("${app.public-base-url:}")
    private String configuredPublicBaseUrl;

    @Value("${app.media.public-base-url:}")
    private String mediaPublicBaseUrl;

    @Value("${app.media.hint:}")
    private String mediaHint;

    public HomeController(UserRepository userRepository,
                          MatchHistoryRepository matchHistoryRepository,
                          GameService gameService,
                          WheelService wheelService,
                          UserService userService,
                          FriendService friendService,
                          LootboxService lootboxService,
                          OwnedItemRepository ownedItemRepository,
                          Environment environment,
                          ModerationService moderationService,
                          ProfileCommentService profileCommentService,
                          AchievementService achievementService) {
        this.userRepository = userRepository;
        this.matchHistoryRepository = matchHistoryRepository;
        this.gameService = gameService;
        this.wheelService = wheelService;
        this.userService = userService;
        this.friendService = friendService;
        this.lootboxService = lootboxService;
        this.ownedItemRepository = ownedItemRepository;
        this.environment = environment;
        this.moderationService = moderationService;
        this.profileCommentService = profileCommentService;
        this.achievementService = achievementService;
    }

    @GetMapping("/")
    public String home(Model model) {
        var ranking = userService.topPlayers();
        model.addAttribute("ranking", ranking != null ? ranking : List.of());
        return "index";
    }

    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication,
                            @RequestParam(value = "q", required = false) String query,
                            HttpServletRequest request,
                            Model model) {

        User user = userRepository.findByUsername(authentication.getName()).orElseThrow();
        lootboxService.grantDailyIfNeeded(user.getUsername());
        achievementService.checkAndAward(user.getId());
        user = userRepository.findByUsername(authentication.getName()).orElseThrow();
        model.addAttribute("user", user);
        model.addAttribute("stats", user.getStatistics());

        var unlockedAch = achievementService.forUser(user.getId());
        Map<pl.pb.monopoly.domain.AchievementType, java.time.LocalDateTime> achMap = new java.util.HashMap<>();
        for (pl.pb.monopoly.domain.Achievement a : unlockedAch) achMap.put(a.getCode(), a.getUnlockedAt());
        List<pl.pb.monopoly.dto.AchievementView> achievements = new java.util.ArrayList<>();
        for (pl.pb.monopoly.domain.AchievementType t : pl.pb.monopoly.domain.AchievementType.values()) {
            achievements.add(new pl.pb.monopoly.dto.AchievementView(
                    t.title, t.description, t.coinReward, t.iconClass, achMap.containsKey(t), achMap.get(t)));
        }
        model.addAttribute("achievements", achievements);
        model.addAttribute("achievementsUnlocked", unlockedAch.size());
        model.addAttribute("achievementsTotal", pl.pb.monopoly.domain.AchievementType.values().length);
        model.addAttribute("matches", matchHistoryRepository.findByUserIdOrderByPlayedAtDesc(user.getId())
                .stream().limit(6).toList());
        var activeSessions = gameService.myActiveSessions(user.getUsername());
        model.addAttribute("activeSessions", activeSessions);
        model.addAttribute("gameUrl", activeSessions.isEmpty()
                ? "/game"
                : (activeSessions.get(0).getStatus() == GameStatus.WAITING
                    ? "/game"
                    : "/game/" + activeSessions.get(0).getId()));
        model.addAttribute("canSpin", wheelService.canSpinToday(user.getUsername()));
        model.addAttribute("friends", friendService.friendsOf(user.getUsername()));
        model.addAttribute("requests", friendService.pendingFor(user.getUsername()));
        model.addAttribute("query", query);
        model.addAttribute("results", friendService.search(query, user.getUsername()));
        model.addAttribute("availableBoxes", user.getStatistics() != null
                ? user.getStatistics().getAvailableLootboxes() : 0);
        model.addAttribute("coins", user.getCoins());
        model.addAttribute("shopBoxes", LootboxService.shopBoxes());
        model.addAttribute("inventory", buildInventory(user));

        model.addAttribute("comments", profileCommentService.commentsFor(user, user));
        model.addAttribute("commentFrom", "/dashboard");
        model.addAttribute("wheelSegments", WheelService.rewardLabels());
        String publicBaseUrl = PublicUrlHelper.publicBaseUrl(request);
        if (publicBaseUrl.isBlank()) {
            publicBaseUrl = configuredPublicBaseUrl != null ? configuredPublicBaseUrl : "";
        }
        model.addAttribute("publicBaseUrl", publicBaseUrl);
        model.addAttribute("mediaPublicBaseUrl",
                PublicUrlHelper.mediaPublicBaseUrl(request, mediaPublicBaseUrl));
        model.addAttribute("mediaHint", mediaHint);
        model.addAttribute("lanServer", environment.acceptsProfiles(org.springframework.core.env.Profiles.of("lan")));

        String equippedFrame = ownedItemRepository.findByUserIdAndEquipped(user.getId(), true)
                .stream()
                .map(OwnedItem::getItemSlug)
                .filter(s -> s.startsWith("frame-"))
                .findFirst().orElse(null);
        model.addAttribute("equippedFrame", equippedFrame);

        String equippedTitleName = ownedItemRepository.findByUserIdAndEquipped(user.getId(), true)
                .stream()
                .map(OwnedItem::getItemSlug)
                .map(LootboxService::findBySlug)
                .filter(t -> t != null && "Tytul".equals(t.category()))
                .map(LootboxItem::name)
                .findFirst().orElse(null);
        model.addAttribute("equippedTitleName", equippedTitleName);

        boolean isMod = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_MODERATOR")
                        || a.getAuthority().equals("ROLE_ADMIN"));
        model.addAttribute("isMod", isMod);
        model.addAttribute("myWarnings", moderationService.warningsForUser(user.getId()));

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
