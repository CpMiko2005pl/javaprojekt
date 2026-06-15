package pl.pb.monopoly.controller;

import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pl.pb.monopoly.domain.GameStatus;
import pl.pb.monopoly.domain.OwnedItem;
import pl.pb.monopoly.domain.Role;
import pl.pb.monopoly.domain.User;
import pl.pb.monopoly.repository.GameSessionRepository;
import pl.pb.monopoly.repository.MatchHistoryRepository;
import pl.pb.monopoly.repository.OwnedItemRepository;
import pl.pb.monopoly.repository.UserRepository;
import pl.pb.monopoly.service.LootboxService;
import pl.pb.monopoly.service.ProfileMediaService;
import pl.pb.monopoly.service.UserService;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Panel administracyjny (tylko ROLE_ADMIN).
 * Admin ma wszystkie funkcje moderatora (podglad i konczenie aktywnych gier,
 * weryfikacja kont) oraz dodatkowe: zmiana rol, usuwanie kont, edycja ekwipunku.
 */
@Controller
@RequestMapping("/admin")
public class AdminController {

    private final UserService userService;
    private final OwnedItemRepository ownedItemRepository;
    private final UserRepository userRepository;
    private final LootboxService lootboxService;
    private final GameSessionRepository gameSessionRepository;
    private final MatchHistoryRepository matchHistoryRepository;
    private final ProfileMediaService profileMediaService;
    private final pl.pb.monopoly.service.GameService gameService;

    public AdminController(UserService userService,
                           OwnedItemRepository ownedItemRepository,
                           UserRepository userRepository,
                           LootboxService lootboxService,
                           GameSessionRepository gameSessionRepository,
                           MatchHistoryRepository matchHistoryRepository,
                           ProfileMediaService profileMediaService,
                           pl.pb.monopoly.service.GameService gameService) {
        this.userService = userService;
        this.ownedItemRepository = ownedItemRepository;
        this.userRepository = userRepository;
        this.lootboxService = lootboxService;
        this.gameSessionRepository = gameSessionRepository;
        this.matchHistoryRepository = matchHistoryRepository;
        this.profileMediaService = profileMediaService;
        this.gameService = gameService;
    }

    @GetMapping("/users")
    public String users(Model model) {
        model.addAttribute("users", userService.findAll());
        model.addAttribute("roles", Role.values());
        model.addAttribute("activeSessions", gameSessionRepository.findAllActive());
        model.addAttribute("totalMatches", matchHistoryRepository.count());
        return "admin/users";
    }

    /** Funkcja moderatora: admin moze zakonczyc dowolna aktywna sesje gry. */
    @PostMapping("/sessions/{id}/end")
    public String endSession(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        // Aktywna gra zyje w RAM — konczymy przez serwis (zapis koncowy + broadcast + usuniecie z RAM).
        gameService.endSessionByAdmin(id);
        redirectAttributes.addFlashAttribute("message", "Sesja zakonczona przez administratora.");
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/verify")
    public String verify(@PathVariable Long id,
                         @RequestParam(defaultValue = "true") boolean verified,
                         RedirectAttributes redirectAttributes) {
        userService.setVerified(id, verified);
        redirectAttributes.addFlashAttribute("message",
                verified ? "Konto zweryfikowane." : "Cofnieto weryfikacje konta.");
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/role")
    public String changeRole(@PathVariable Long id,
                             @RequestParam Role role,
                             RedirectAttributes redirectAttributes) {
        userService.changeRole(id, role);
        redirectAttributes.addFlashAttribute("message", "Zmieniono role na " + role.getDisplayName() + ".");
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        userService.delete(id);
        redirectAttributes.addFlashAttribute("message", "Usunieto uzytkownika.");
        return "redirect:/admin/users";
    }

    // ===== EKWIPUNEK GRACZA =====

    @GetMapping("/users/{id}/inventory")
    public String inventory(@PathVariable Long id, Model model) {
        User user = userService.getById(id);
        List<OwnedItem> items = ownedItemRepository.findByUserIdOrderByObtainedAtDesc(id);
        Map<String, LootboxService.LootboxItem> bySlug = LootboxService.bySlug();

        List<Map<String, Object>> enriched = items.stream().map(o -> {
            LootboxService.LootboxItem t = bySlug.get(o.getItemSlug());
            Map<String, Object> m = new HashMap<>();
            m.put("ownedId", o.getId());
            m.put("slug", o.getItemSlug());
            m.put("equipped", o.isEquipped());
            m.put("obtainedAt", o.getObtainedAt());
            if (t != null) {
                m.put("name", t.name());
                m.put("rarity", t.rarity().name());
                m.put("rarityLabel", t.rarity().label);
                m.put("rarityColor", t.rarity().color);
                m.put("category", t.category());
                m.put("iconClass", t.iconClass());
            } else {
                m.put("name", o.getItemSlug());
                m.put("rarity", "COMMON");
                m.put("rarityLabel", "Common");
                m.put("rarityColor", "#9ca3af");
                m.put("iconClass", "fa-solid fa-cube");
                m.put("category", "Nieznany");
            }
            return m;
        }).toList();

        model.addAttribute("targetUser", user);
        model.addAttribute("inventory", enriched);
        model.addAttribute("catalog", LootboxService.catalog());
        return "admin/inventory";
    }

    /** Admin dodaje item do ekwipunku gracza (po slugu z katalogu). */
    @PostMapping("/users/{id}/inventory/add")
    public String addItem(@PathVariable Long id,
                          @RequestParam String slug,
                          RedirectAttributes redirectAttributes) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Brak uzytkownika"));
        ownedItemRepository.save(new OwnedItem(user, slug));
        redirectAttributes.addFlashAttribute("message", "Dodano item [" + slug + "] do ekwipunku gracza.");
        return "redirect:/admin/users/" + id + "/inventory";
    }

    /** Admin usuwa item z ekwipunku gracza. */
    @PostMapping("/users/{id}/inventory/{itemId}/delete")
    public String removeItem(@PathVariable Long id,
                             @PathVariable Long itemId,
                             RedirectAttributes redirectAttributes) {
        OwnedItem item = ownedItemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Brak itemu"));
        if (!item.getUser().getId().equals(id)) {
            redirectAttributes.addFlashAttribute("error", "Item nie nalezy do tego gracza.");
            return "redirect:/admin/users/" + id + "/inventory";
        }
        ownedItemRepository.deleteById(itemId);
        redirectAttributes.addFlashAttribute("message", "Usunieto item z ekwipunku.");
        return "redirect:/admin/users/" + id + "/inventory";
    }

    /** Admin daje graczowi skrzynke (zwieksza availableLootboxes o 1). */
    @PostMapping("/users/{id}/give-lootbox")
    public String giveLootbox(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        User user = userService.getById(id);
        if (user.getStatistics() != null) {
            user.getStatistics().setAvailableLootboxes(
                    Math.min(user.getStatistics().getAvailableLootboxes() + 1, 10));
            userRepository.save(user);
            redirectAttributes.addFlashAttribute("message", "Dodano skrzynke graczowi " + user.getUsername());
        }
        return "redirect:/admin/users";
    }

    /** Admin dolicza (lub odejmuje, gdy ujemne) monety graczowi. */
    @PostMapping("/users/{id}/add-coins")
    public String addCoins(@PathVariable Long id,
                           @RequestParam int amount,
                           RedirectAttributes redirectAttributes) {
        User user = userService.getById(id);
        user.addCoins(amount);
        userRepository.save(user);
        redirectAttributes.addFlashAttribute("message",
                (amount >= 0 ? "Dodano " + amount : "Odjeto " + (-amount))
                        + " monet graczowi " + user.getUsername() + " (stan: " + user.getCoins() + ").");
        return "redirect:/admin/users";
    }

    /** Podglad przeslanej legitymacji gracza — tylko admin. */
    @GetMapping("/users/{id}/legitymacja")
    public ResponseEntity<Resource> viewLegitymacja(@PathVariable Long id) {
        User user = userService.getById(id);
        String rel = user.getVerificationDocUrl();
        if (rel == null || rel.isBlank()) {
            return ResponseEntity.notFound().build();
        }
        Path file = profileMediaService.resolveUpload(rel);
        // ochrona przed wyjsciem poza katalog uploads
        Path base = profileMediaService.resolveUpload("verification");
        if (!file.startsWith(base) || !Files.isReadable(file)) {
            return ResponseEntity.notFound().build();
        }
        MediaType type = rel.endsWith(".pdf") ? MediaType.APPLICATION_PDF
                : rel.endsWith(".png") ? MediaType.IMAGE_PNG
                : rel.endsWith(".webp") ? MediaType.parseMediaType("image/webp")
                : MediaType.IMAGE_JPEG;
        return ResponseEntity.ok()
                .contentType(type)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + file.getFileName() + "\"")
                .body(new PathResource(file));
    }
}
