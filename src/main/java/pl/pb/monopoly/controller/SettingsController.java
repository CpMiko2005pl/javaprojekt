package pl.pb.monopoly.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pl.pb.monopoly.domain.User;
import pl.pb.monopoly.repository.UserRepository;
import pl.pb.monopoly.service.UserService;

@Controller
@RequestMapping("/settings")
public class SettingsController {

    private final UserRepository userRepository;
    private final UserService userService;

    public SettingsController(UserRepository userRepository, UserService userService) {
        this.userRepository = userRepository;
        this.userService = userService;
    }

    @GetMapping
    public String settings(Authentication auth, Model model) {
        User user = userRepository.findByUsername(auth.getName()).orElseThrow();
        model.addAttribute("user", user);
        return "settings";
    }

    @PostMapping("/profile")
    public String updateProfile(Authentication auth,
                                @RequestParam String email,
                                @RequestParam(required = false) String bio,
                                @RequestParam(required = false) String bannerUrl,
                                @RequestParam(required = false) String avatarUrl,
                                RedirectAttributes ra) {
        try {
            userService.updateProfile(auth.getName(), email, bio, bannerUrl, avatarUrl);
            ra.addFlashAttribute("message", "Profil został zaktualizowany.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/settings";
    }

    @PostMapping("/password")
    public String changePassword(Authentication auth,
                                 @RequestParam String currentPassword,
                                 @RequestParam String newPassword,
                                 @RequestParam String confirmPassword,
                                 RedirectAttributes ra) {
        if (!newPassword.equals(confirmPassword)) {
            ra.addFlashAttribute("error", "Nowe hasła nie są takie same.");
            return "redirect:/settings";
        }
        try {
            userService.changePassword(auth.getName(), currentPassword, newPassword);
            ra.addFlashAttribute("message", "Hasło zostało zmienione.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/settings";
    }
}
