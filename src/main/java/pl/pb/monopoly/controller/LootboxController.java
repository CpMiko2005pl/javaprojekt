package pl.pb.monopoly.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.pb.monopoly.domain.User;
import pl.pb.monopoly.repository.UserRepository;
import pl.pb.monopoly.service.LootboxService;
import pl.pb.monopoly.service.LootboxService.LootboxItem;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST endpoint do otwierania skrzynki - zwraca wylosowany item i wirtualna
 * "tasme" itemow do animacji karuzeli (CS2-style) na froncie.
 */
@RestController
public class LootboxController {

    private final LootboxService lootboxService;
    private final UserRepository userRepository;

    public LootboxController(LootboxService lootboxService, UserRepository userRepository) {
        this.lootboxService = lootboxService;
        this.userRepository = userRepository;
    }

    @PostMapping("/lootbox/open")
    public ResponseEntity<?> open(Authentication auth) {
        try {
            LootboxItem winner = lootboxService.open(auth.getName());
            User user = userRepository.findByUsername(auth.getName()).orElseThrow();

            List<LootboxItem> strip = lootboxService.rollVisualStrip(winner);

            Map<String, Object> response = new HashMap<>();
            response.put("winner", toMap(winner));
            response.put("strip", strip.stream().map(this::toMap).toList());
            response.put("availableBoxes", user.getStatistics() != null
                    ? user.getStatistics().getAvailableLootboxes() : 0);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    private Map<String, Object> toMap(LootboxItem i) {
        Map<String, Object> m = new HashMap<>();
        m.put("slug", i.slug());
        m.put("name", i.name());
        m.put("rarity", i.rarity().name());
        m.put("rarityLabel", i.rarity().label);
        m.put("rarityColor", i.rarity().color);
        m.put("category", i.category());
        m.put("iconClass", i.iconClass());
        m.put("description", i.description());
        return m;
    }
}
