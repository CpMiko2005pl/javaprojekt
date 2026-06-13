package pl.pb.monopoly.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;
import pl.pb.monopoly.repository.OwnedItemRepository;
import pl.pb.monopoly.repository.UserRepository;
import pl.pb.monopoly.service.LootboxService;

import java.net.URI;

import java.util.Map;

/**
 * Proxy do DiceBear API — generowanie awatarow SVG.
 * Uwzglednia zalozony awatar z ekwipunku (inny styl/seed).
 */
@RestController
public class AvatarController {

    private static final Map<String, String> AVATAR_STYLE = Map.of(
            "avatar-student", "pixel-art",
            "avatar-zmeczony", "bottts-neutral",
            "avatar-bibliotekarz", "lorelei",
            "avatar-gwint", "fun-emoji",
            "avatar-dziekan", "notionists",
            "avatar-rektor", "adventurer"
    );

    private static final Map<String, String> AVATAR_SEED_SUFFIX = Map.of(
            "avatar-student", "",
            "avatar-zmeczony", "-tired",
            "avatar-bibliotekarz", "-books",
            "avatar-gwint", "-cards",
            "avatar-dziekan", "-dean",
            "avatar-rektor", "-rector"
    );

    private final RestClient restClient = RestClient.create();
    private final UserRepository userRepository;
    private final OwnedItemRepository ownedItemRepository;

    public AvatarController(UserRepository userRepository, OwnedItemRepository ownedItemRepository) {
        this.userRepository = userRepository;
        this.ownedItemRepository = ownedItemRepository;
    }

    @GetMapping("/api/avatar/{username}")
    public ResponseEntity<byte[]> avatar(@PathVariable String username) {
        try {
            var userOpt = userRepository.findByUsername(username);

            // Jezeli uzytkownik ma ustawiony wlasny URL awatara — przekieruj tam
            if (userOpt.isPresent()) {
                String customUrl = userOpt.get().getAvatarUrl();
                if (customUrl != null && !customUrl.isBlank()) {
                    return ResponseEntity.status(HttpStatus.FOUND)
                            .location(URI.create(customUrl))
                            .build();
                }
            }

            // Standardowy awatar z DiceBear (z uwzglednieniem ekwipunku)
            String style = "pixel-art";
            String seed = username;

            if (userOpt.isPresent()) {
                var equippedAvatar = ownedItemRepository.findByUserIdAndEquipped(userOpt.get().getId(), true)
                        .stream()
                        .map(item -> LootboxService.findBySlug(item.getItemSlug()))
                        .filter(t -> t != null && "Awatar".equals(t.category()))
                        .findFirst();
                if (equippedAvatar.isPresent()) {
                    String slug = equippedAvatar.get().slug();
                    style = AVATAR_STYLE.getOrDefault(slug, style);
                    seed = username + AVATAR_SEED_SUFFIX.getOrDefault(slug, "");
                }
            }

            byte[] svg = restClient.get()
                    .uri("https://api.dicebear.com/9.x/{style}/svg?seed={seed}" +
                         "&backgroundColor=1e293b&radius=50", style, seed)
                    .retrieve()
                    .body(byte[].class);
            return ResponseEntity.ok()
                    .contentType(MediaType.valueOf("image/svg+xml"))
                    .header("Cache-Control", "public, max-age=300")
                    .body(svg);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}
