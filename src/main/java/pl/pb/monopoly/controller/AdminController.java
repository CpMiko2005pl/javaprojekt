package pl.pb.monopoly.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pl.pb.monopoly.domain.GameStatus;
import pl.pb.monopoly.domain.OwnedItem;
import pl.pb.monopoly.domain.Role;
import pl.pb.monopoly.domain.User;
import pl.pb.monopoly.dto.AdminUserEditForm;
import pl.pb.monopoly.repository.GameSessionRepository;
import pl.pb.monopoly.repository.MatchHistoryRepository;
import pl.pb.monopoly.repository.OwnedItemRepository;
import pl.pb.monopoly.repository.UserRepository;
import pl.pb.monopoly.service.LootboxService;
import pl.pb.monopoly.service.ProfileMediaService;
import pl.pb.monopoly.service.UserService;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    private static final List<String> SORT_KEYS = List.of("username", "createdAt", "coins");

    @GetMapping("/users")
    public String users(@RequestParam(required = false) String sort,
                        @RequestParam(required = false) String dir,
                        @RequestParam(required = false)
                        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
                        @RequestParam(required = false) Role role,
                        @CookieValue(value = "adminUserSort", required = false) String sortCookie,
                        @CookieValue(value = "adminUserDir", required = false) String dirCookie,
                        HttpServletResponse response,
                        Model model) {

        String effSort = sort != null ? sort : (sortCookie != null ? sortCookie : "username");
        String effDir = dir != null ? dir : (dirCookie != null ? dirCookie : "asc");
        if (!SORT_KEYS.contains(effSort)) effSort = "username";
        if (!"asc".equalsIgnoreCase(effDir) && !"desc".equalsIgnoreCase(effDir)) effDir = "asc";

        if (sort != null) writeCookie(response, "adminUserSort", effSort);
        if (dir != null) writeCookie(response, "adminUserDir", effDir);

        model.addAttribute("users", userService.findForAdmin(effSort, effDir, dateFrom, role));
        model.addAttribute("roles", Role.values());
        model.addAttribute("sort", effSort);
        model.addAttribute("dir", effDir);
        model.addAttribute("dateFrom", dateFrom);
        model.addAttribute("selectedRole", role);
        model.addAttribute("activeSessions", gameSessionRepository.findAllActive());
        model.addAttribute("totalMatches", matchHistoryRepository.count());
        return "admin/users";
    }

    private void writeCookie(HttpServletResponse response, String name, String value) {
        Cookie cookie = new Cookie(name, value);
        cookie.setMaxAge(30 * 24 * 60 * 60);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        response.addCookie(cookie);
    }

    @GetMapping("/users/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        User u = userService.getById(id);
        if (!model.containsAttribute("form")) {
            AdminUserEditForm form = new AdminUserEditForm();
            form.setEmail(u.getEmail());
            form.setFirstName(u.getFirstName());
            form.setLastName(u.getLastName());
            form.setDateOfBirth(u.getDateOfBirth());
            form.setCoins(u.getCoins());
            form.setBio(u.getBio());
            model.addAttribute("form", form);
        }
        model.addAttribute("editUser", u);
        return "admin/user-edit";
    }

    @PostMapping("/users/{id}/edit")
    public String editSave(@PathVariable Long id,
                           @Valid @ModelAttribute("form") AdminUserEditForm form,
                           BindingResult bindingResult,
                           Model model,
                           RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("editUser", userService.getById(id));
            return "admin/user-edit";
        }
        try {
            userService.updateByAdmin(id, form);
            redirectAttributes.addFlashAttribute("message", "Zapisano dane uzytkownika.");
            return "redirect:/admin/users";
        } catch (IllegalArgumentException ex) {
            bindingResult.rejectValue("email", "taken", ex.getMessage());
            model.addAttribute("editUser", userService.getById(id));
            return "admin/user-edit";
        }
    }

    @PostMapping("/sessions/{id}/end")
    public String endSession(@PathVariable Long id, RedirectAttributes redirectAttributes) {

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

    @GetMapping("/users/{id}/legitymacja")
    public ResponseEntity<Resource> viewLegitymacja(@PathVariable Long id) {
        User user = userService.getById(id);
        String rel = user.getVerificationDocUrl();
        if (rel == null || rel.isBlank()) {
            return ResponseEntity.notFound().build();
        }
        Path file = profileMediaService.resolveUpload(rel);

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
