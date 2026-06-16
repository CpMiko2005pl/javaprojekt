package pl.pb.monopoly.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.pb.monopoly.domain.HandCardType;
import pl.pb.monopoly.domain.OwnedItem;
import pl.pb.monopoly.domain.PlayerStatistics;
import pl.pb.monopoly.domain.User;
import pl.pb.monopoly.repository.OwnedItemRepository;
import pl.pb.monopoly.repository.UserRepository;
import pl.pb.monopoly.service.LootboxService;
import pl.pb.monopoly.service.LootboxService.LootboxItem;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class LootboxController {

    private final LootboxService lootboxService;
    private final UserRepository userRepository;
    private final OwnedItemRepository ownedItemRepository;

    public LootboxController(LootboxService lootboxService, UserRepository userRepository,
                             OwnedItemRepository ownedItemRepository) {
        this.lootboxService = lootboxService;
        this.userRepository = userRepository;
        this.ownedItemRepository = ownedItemRepository;
    }

    @PostMapping("/shop/buy/{box}")
    public ResponseEntity<?> buyBox(@PathVariable String box, Authentication auth) {
        try {
            LootboxService.BuyResult result = lootboxService.buyBox(auth.getName(), box);
            LootboxItem winner = result.item();
            List<LootboxItem> strip = lootboxService.rollVisualStrip(winner, lootboxService.poolForBox(box));

            Map<String, Object> response = new HashMap<>();
            response.put("winner", toMap(winner));
            response.put("strip", strip.stream().map(this::toMap).toList());
            response.put("addedToInventory", result.addedToInventory());
            if (result.inventoryNote() != null) {
                response.put("inventoryNote", result.inventoryNote());
            }
            response.put("coins", result.coinsLeft());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @PostMapping("/lootbox/open")
    public ResponseEntity<?> open(Authentication auth) {
        try {
            LootboxService.OpenResult result = lootboxService.open(auth.getName());
            LootboxItem winner = result.item();
            User user = userRepository.findByUsername(auth.getName()).orElseThrow();

            List<LootboxItem> strip = lootboxService.rollVisualStrip(winner);

            Map<String, Object> response = new HashMap<>();
            response.put("winner", toMap(winner));
            response.put("strip", strip.stream().map(this::toMap).toList());
            response.put("addedToInventory", result.addedToInventory());
            if (result.inventoryNote() != null) {
                response.put("inventoryNote", result.inventoryNote());
            }
            response.put("availableBoxes", user.getStatistics() != null
                    ? user.getStatistics().getAvailableLootboxes() : 0);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @PostMapping("/inventory/{id}/equip")
    @Transactional
    public ResponseEntity<?> equip(@PathVariable Long id, Authentication auth) {
        User user = userRepository.findByUsername(auth.getName()).orElseThrow();
        OwnedItem item = ownedItemRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Brak przedmiotu"));
        if (!item.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(403).body(Map.of("error", "To nie Twoj przedmiot."));
        }
        LootboxItem catalog = LootboxService.findBySlug(item.getItemSlug());
        String category = catalog != null ? catalog.category() : "";
        String slot = LootboxService.equipSlot(category);
        boolean willEquip = !item.isEquipped();
        if (willEquip) {

            ownedItemRepository.findByUserIdOrderByObtainedAtDesc(user.getId()).stream()
                    .filter(o -> !o.getId().equals(id))
                    .filter(o -> {
                        LootboxItem c = LootboxService.findBySlug(o.getItemSlug());
                        return c != null && LootboxService.equipSlot(c.category()).equals(slot);
                    })
                    .forEach(o -> { o.setEquipped(false); ownedItemRepository.save(o); });
        }
        item.setEquipped(willEquip);
        ownedItemRepository.save(item);

        if ("Karta bonusowa".equals(category)) {
            HandCardType cardType = LootboxService.BONUS_CARD_MAP.get(item.getItemSlug());
            PlayerStatistics stats = user.getStatistics();
            if (cardType != null && stats != null) {
                if (willEquip) {
                    stats.setPendingWheelCard(cardType.name());
                } else if (cardType.name().equals(stats.getPendingWheelCard())) {
                    stats.setPendingWheelCard(null);
                }
            }
        }

        String message = willEquip
                ? switch (category) {
                    case "Kolor pionka" -> "Kolor pionka zalozony — widoczny w nastepnej/rozpoczetej grze.";
                    case "Pionek 3D" -> "Pionek 3D zalozony — widoczny na planszy w aktywnych grach.";
                    case "Awatar" -> "Awatar zalozony — odswiez profil, by zobaczyc zmiane.";
                    case "Ramka" -> "Ramka profilu zalozona.";
                    case "Tytul" -> "Tytul zalozony — wyswietli sie na profilu.";
                    case "Karta bonusowa" -> "Karta bonusowa dodana — pojawi sie w rece na START nastepnej gry.";
                    default -> "Przedmiot zalozony.";
                }
                : "Przedmiot zdjety.";

        return ResponseEntity.ok(Map.of(
                "equipped", willEquip,
                "itemId", id,
                "category", category,
                "message", message
        ));
    }

    @PostMapping("/inventory/{id}/delete")
    @Transactional
    public ResponseEntity<?> delete(@PathVariable Long id, Authentication auth) {
        User user = userRepository.findByUsername(auth.getName()).orElseThrow();
        OwnedItem item = ownedItemRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Brak przedmiotu"));
        if (!item.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(403).body(Map.of("error", "To nie Twoj przedmiot."));
        }

        if (item.isEquipped()) {
            LootboxItem catalog = LootboxService.findBySlug(item.getItemSlug());
            String category = catalog != null ? catalog.category() : "";
            if ("Karta bonusowa".equals(category)) {
                HandCardType cardType = LootboxService.BONUS_CARD_MAP.get(item.getItemSlug());
                PlayerStatistics stats = user.getStatistics();
                if (cardType != null && stats != null && cardType.name().equals(stats.getPendingWheelCard())) {
                    stats.setPendingWheelCard(null);
                }
            }
        }

        ownedItemRepository.delete(item);
        return ResponseEntity.ok(Map.of("success", true, "message", "Przedmiot usuniety z ekwipunku."));
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
