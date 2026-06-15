package pl.pb.monopoly.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pl.pb.monopoly.domain.User;
import pl.pb.monopoly.repository.UserRepository;
import pl.pb.monopoly.service.ProfileMediaService;
import pl.pb.monopoly.service.UserService;
import pl.pb.monopoly.util.PublicUrlHelper;

@Controller
@RequestMapping("/settings")
public class SettingsController {

    private final UserRepository userRepository;
    private final UserService userService;
    private final ProfileMediaService profileMediaService;

    @Value("${app.media.public-base-url:}")
    private String mediaPublicBaseUrl;

    @Value("${app.public-base-url:}")
    private String configuredPublicBaseUrl;

    @Value("${app.media.hint:}")
    private String mediaHint;

    public SettingsController(UserRepository userRepository,
                              UserService userService,
                              ProfileMediaService profileMediaService) {
        this.userRepository = userRepository;
        this.userService = userService;
        this.profileMediaService = profileMediaService;
    }

    @GetMapping
    public String settings(Authentication auth, HttpServletRequest request, Model model) {
        User user = userRepository.findByUsername(auth.getName()).orElseThrow();
        model.addAttribute("user", user);
        model.addAttribute("mediaPublicBaseUrl",
                PublicUrlHelper.mediaPublicBaseUrl(request, mediaPublicBaseUrl));
        model.addAttribute("mediaHint", mediaHint);
        return "settings";
    }

    @PostMapping("/profile")
    public String updateProfile(Authentication auth,
                                HttpServletRequest request,
                                @RequestParam String email,
                                @RequestParam(required = false) String bio,
                                @RequestParam(required = false) String avatarUrl,
                                @RequestParam(required = false) String bannerUrl,
                                @RequestParam(required = false) String backgroundUrl,
                                @RequestParam(required = false) MultipartFile avatarFile,
                                @RequestParam(required = false) MultipartFile bannerFile,
                                @RequestParam(required = false) MultipartFile backgroundFile,
                                RedirectAttributes ra) {
        try {
            User user = userRepository.findByUsername(auth.getName()).orElseThrow();
            String resolvedAvatarUrl;
            String resolvedBannerUrl;
            String resolvedBackgroundUrl;
            String publicBase = resolvePublicBaseUrl(request);

            if (avatarFile != null && !avatarFile.isEmpty()) {
                resolvedAvatarUrl = profileMediaService.saveAvatar(user.getUsername(), avatarFile, publicBase);
            } else {
                resolvedAvatarUrl = blankToNull(avatarUrl);
            }

            if (bannerFile != null && !bannerFile.isEmpty()) {
                resolvedBannerUrl = profileMediaService.saveBanner(user.getUsername(), bannerFile, publicBase);
            } else {
                resolvedBannerUrl = blankToNull(bannerUrl);
            }

            if (backgroundFile != null && !backgroundFile.isEmpty()) {
                resolvedBackgroundUrl = profileMediaService.saveBackground(user.getUsername(), backgroundFile, publicBase);
            } else {
                resolvedBackgroundUrl = blankToNull(backgroundUrl);
            }

            userService.updateProfile(auth.getName(), email, bio, resolvedBannerUrl, resolvedAvatarUrl, resolvedBackgroundUrl);
            ra.addFlashAttribute("message", "Profil został zaktualizowany.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/settings";
    }

    @PostMapping("/verify")
    public String requestVerification(Authentication auth,
                                      @RequestParam(required = false) MultipartFile legitymacjaFile,
                                      RedirectAttributes ra) {
        try {
            User user = userRepository.findByUsername(auth.getName()).orElseThrow();
            if (user.isVerified()) {
                ra.addFlashAttribute("message", "Twoje konto jest już zweryfikowane.");
                return "redirect:/settings";
            }
            if (legitymacjaFile == null || legitymacjaFile.isEmpty()) {
                throw new IllegalArgumentException("Nie wybrano pliku legitymacji.");
            }
            String path = profileMediaService.saveVerificationDoc(user.getUsername(), legitymacjaFile);
            user.setVerificationDocUrl(path);
            userRepository.save(user);
            ra.addFlashAttribute("message", "Legitymacja przesłana. Czeka na akceptację administratora.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/settings";
    }

    private static String blankToNull(String value) {
        return value != null && !value.isBlank() ? value.strip() : null;
    }

    private String resolvePublicBaseUrl(HttpServletRequest request) {
        String base = PublicUrlHelper.publicBaseUrl(request);
        if (base.isBlank() && configuredPublicBaseUrl != null && !configuredPublicBaseUrl.isBlank()) {
            base = configuredPublicBaseUrl.strip();
        }
        return base;
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
