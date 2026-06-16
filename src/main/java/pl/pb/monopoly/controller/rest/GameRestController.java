package pl.pb.monopoly.controller.rest;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import pl.pb.monopoly.dto.GameStateDto;
import pl.pb.monopoly.dto.SellPropertyRequest;
import pl.pb.monopoly.dto.TransferRequest;
import pl.pb.monopoly.service.GameService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/game")
public class GameRestController {

    private final GameService gameService;

    public GameRestController(GameService gameService) {
        this.gameService = gameService;
    }

    @GetMapping("/{id}/state")
    public GameStateDto state(@PathVariable Long id, Authentication auth) {
        return gameService.getState(id, auth.getName());
    }

    @PostMapping("/{id}/roll")
    public ResponseEntity<?> roll(@PathVariable Long id, Authentication auth) {
        try {
            return ResponseEntity.ok(gameService.roll(id, auth.getName()));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @PostMapping("/{id}/transfer")
    public ResponseEntity<?> transfer(@PathVariable Long id,
                                      @RequestBody TransferRequest req,
                                      Authentication auth) {
        try {
            return ResponseEntity.ok(gameService.transfer(id, auth.getName(), req.toPlayerId(), req.amount()));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @PostMapping("/{id}/buy")
    public ResponseEntity<?> buy(@PathVariable Long id, Authentication auth) {
        try {
            return ResponseEntity.ok(gameService.buy(id, auth.getName()));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @PostMapping("/{id}/skip")
    public ResponseEntity<?> skip(@PathVariable Long id, Authentication auth) {
        try {
            return ResponseEntity.ok(gameService.skipPurchase(id, auth.getName()));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @PostMapping("/{id}/bid")
    public ResponseEntity<?> bid(@PathVariable Long id,
                                 @RequestBody Map<String, Integer> body,
                                 Authentication auth) {
        try {
            int amount = body.getOrDefault("amount", 0);
            return ResponseEntity.ok(gameService.bid(id, auth.getName(), amount));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @PostMapping("/{id}/sell")
    public ResponseEntity<?> sell(@PathVariable Long id,
                                  @RequestBody SellPropertyRequest req,
                                  Authentication auth) {
        try {
            return ResponseEntity.ok(gameService.sellProperty(id, auth.getName(), req.position()));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @PostMapping("/{id}/pay-debt")
    public ResponseEntity<?> payDebt(@PathVariable Long id, Authentication auth) {
        try {
            return ResponseEntity.ok(gameService.payDebt(id, auth.getName()));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @PostMapping("/{id}/bankrupt")
    public ResponseEntity<?> bankrupt(@PathVariable Long id, Authentication auth) {
        try {
            return ResponseEntity.ok(gameService.declareBankruptcy(id, auth.getName()));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @PostMapping("/{id}/play-card")
    public ResponseEntity<?> playCard(@PathVariable Long id,
                                      @RequestBody Map<String, Object> body,
                                      Authentication auth) {
        try {
            String cardType = (String) body.get("cardType");
            Integer targetPos = body.get("targetPos") != null
                    ? ((Number) body.get("targetPos")).intValue() : null;
            return ResponseEntity.ok(gameService.playCard(id, auth.getName(), cardType, targetPos));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @PostMapping("/{id}/upgrade")
    public ResponseEntity<?> upgrade(@PathVariable Long id, Authentication auth) {
        try {
            return ResponseEntity.ok(gameService.upgrade(id, auth.getName()));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @PostMapping("/{id}/skip-upgrade")
    public ResponseEntity<?> skipUpgrade(@PathVariable Long id, Authentication auth) {
        try {
            return ResponseEntity.ok(gameService.skipUpgrade(id, auth.getName()));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @PostMapping("/{id}/buyback")
    public ResponseEntity<?> buyback(@PathVariable Long id, Authentication auth) {
        try {
            return ResponseEntity.ok(gameService.buybackProperty(id, auth.getName()));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @PostMapping("/{id}/skip-buyback")
    public ResponseEntity<?> skipBuyback(@PathVariable Long id, Authentication auth) {
        try {
            return ResponseEntity.ok(gameService.skipBuyback(id, auth.getName()));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @PostMapping("/{id}/buyout")
    public ResponseEntity<?> buyout(@PathVariable Long id, Authentication auth) {
        try {
            return ResponseEntity.ok(gameService.buyoutTakeover(id, auth.getName()));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @PostMapping("/{id}/skip-buyout")
    public ResponseEntity<?> skipBuyout(@PathVariable Long id, Authentication auth) {
        try {
            return ResponseEntity.ok(gameService.skipTakeover(id, auth.getName()));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @PostMapping("/{id}/ready")
    public ResponseEntity<?> ready(@PathVariable Long id, Authentication auth) {
        try {
            return ResponseEntity.ok(gameService.readyUp(id, auth.getName()));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @PostMapping("/{id}/start")
    public ResponseEntity<?> start(@PathVariable Long id, Authentication auth) {
        try {
            return ResponseEntity.ok(gameService.startGame(id, auth.getName()));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @PostMapping("/{id}/add-bot")
    public ResponseEntity<?> addBot(@PathVariable Long id,
                                    @RequestBody(required = false) Map<String, String> body,
                                    Authentication auth) {
        try {
            String name = body != null ? body.get("name") : null;
            String color = body != null ? body.get("color") : null;
            return ResponseEntity.ok(gameService.addBot(id, auth.getName(), name, color));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @PostMapping("/{id}/remove-bot/{playerId}")
    public ResponseEntity<?> removeBot(@PathVariable Long id,
                                       @PathVariable Long playerId,
                                       Authentication auth) {
        try {
            return ResponseEntity.ok(gameService.removeBot(id, auth.getName(), playerId));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @PostMapping("/{id}/invite")
    public ResponseEntity<?> invite(@PathVariable Long id,
                                    @RequestBody Map<String, Long> body,
                                    Authentication auth) {
        try {
            Long friendId = body != null ? body.get("friendId") : null;
            if (friendId == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Brak gracza do zaproszenia."));
            }
            return ResponseEntity.ok(gameService.inviteFriend(id, auth.getName(), friendId));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @GetMapping("/invites")
    public List<pl.pb.monopoly.dto.GameInviteDto> invites(Authentication auth) {
        return gameService.pendingInvitesFor(auth.getName());
    }

    @PostMapping("/invites/{inviteId}/accept")
    public ResponseEntity<?> acceptInvite(@PathVariable Long inviteId, Authentication auth) {
        try {
            var session = gameService.acceptInvite(inviteId, auth.getName());
            return ResponseEntity.ok(Map.of(
                    "sessionId", session.getId(),
                    "redirect", "/game"
            ));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @PostMapping("/invites/{inviteId}/decline")
    public ResponseEntity<?> declineInvite(@PathVariable Long inviteId, Authentication auth) {
        try {
            gameService.declineInvite(inviteId, auth.getName());
            return ResponseEntity.ok(Map.of("declined", true));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @PostMapping("/{id}/react")
    public ResponseEntity<?> react(@PathVariable Long id, @RequestBody Map<String, String> body, Authentication auth) {
        String code = body.get("code");
        if (code == null) return ResponseEntity.badRequest().build();
        gameService.broadcastReaction(id, auth.getName(), code);
        return ResponseEntity.ok().build();
    }
@PostMapping("/{id}/leave")
    public ResponseEntity<?> leave(@PathVariable Long id, Authentication auth) {
        try {
            gameService.leaveGame(id, auth.getName());
            return ResponseEntity.ok(Map.of("left", true));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }
}
