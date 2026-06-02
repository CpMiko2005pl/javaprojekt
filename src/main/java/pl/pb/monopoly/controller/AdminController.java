package pl.pb.monopoly.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pl.pb.monopoly.domain.Role;
import pl.pb.monopoly.service.UserService;

import java.math.BigDecimal;

/**
 * Panel administracyjny (tylko ROLE_ADMIN - zabezpieczone w SecurityConfig).
 * Realizuje wymagania z deklaracji: lista uzytkownikow, doladowanie srodkow (PLN),
 * weryfikacja konta po legitymacji PB oraz zarzadzanie rolami.
 */
@Controller
@RequestMapping("/admin")
public class AdminController {

    private final UserService userService;

    public AdminController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/users")
    public String users(Model model) {
        model.addAttribute("users", userService.findAll());
        model.addAttribute("roles", Role.values());
        return "admin/users";
    }

    @PostMapping("/users/{id}/topup")
    public String topUp(@PathVariable Long id,
                        @RequestParam BigDecimal amount,
                        RedirectAttributes redirectAttributes) {
        try {
            userService.topUpBalance(id, amount);
            redirectAttributes.addFlashAttribute("message", "Doladowano konto kwota " + amount + " PLN.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
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
}
