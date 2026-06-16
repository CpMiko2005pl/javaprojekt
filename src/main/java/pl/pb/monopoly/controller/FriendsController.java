package pl.pb.monopoly.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pl.pb.monopoly.dto.FriendDto;
import pl.pb.monopoly.service.FriendService;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/friends")
public class FriendsController {

    private final FriendService friendService;

    public FriendsController(FriendService friendService) {
        this.friendService = friendService;
    }

    @GetMapping
    public String friends(@RequestParam(value = "q", required = false) String query,
                          Authentication auth, Model model) {
        String me = auth.getName();
        model.addAttribute("friends", friendService.friendsOf(me));
        model.addAttribute("requests", friendService.pendingFor(me));
        model.addAttribute("query", query);
        model.addAttribute("results", friendService.search(query, me));
        return "friends";
    }

    @GetMapping("/api/search")
    @ResponseBody
    public List<FriendDto> apiSearch(@RequestParam(value = "q", required = false) String q,
                                     Authentication auth) {
        return friendService.search(q, auth.getName());
    }

    @PostMapping("/api/add")
    @ResponseBody
    public ResponseEntity<?> apiAdd(@RequestBody Map<String, String> body, Authentication auth) {
        String username = body.get("username");
        try {
            friendService.sendRequest(auth.getName(), username);
            return ResponseEntity.ok(Map.of("ok", true, "message", "Wysłano zaproszenie do " + username));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @PostMapping("/add")
    public String add(@RequestParam String username,
                      @RequestParam(value = "from", required = false) String from,
                      Authentication auth, RedirectAttributes ra) {
        try {
            friendService.sendRequest(auth.getName(), username);
            ra.addFlashAttribute("message", "Wyslano zaproszenie do: " + username);
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return redirectTarget(from);
    }

    @PostMapping("/{id}/accept")
    public String accept(@PathVariable Long id,
                         @RequestParam(value = "from", required = false) String from,
                         Authentication auth, RedirectAttributes ra) {
        try {
            friendService.accept(id, auth.getName());
            ra.addFlashAttribute("message", "Dodano do znajomych!");
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return redirectTarget(from);
    }

    @PostMapping("/{id}/remove")
    public String remove(@PathVariable Long id,
                         @RequestParam(value = "from", required = false) String from,
                         Authentication auth, RedirectAttributes ra) {
        try {
            friendService.removeOrReject(id, auth.getName());
            ra.addFlashAttribute("message", "Relacja usunieta.");
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }
        return redirectTarget(from);
    }

    private String redirectTarget(String from) {
        if ("dashboard".equalsIgnoreCase(from)) {
            return "redirect:/dashboard";
        }
        return "redirect:/friends";
    }
}
