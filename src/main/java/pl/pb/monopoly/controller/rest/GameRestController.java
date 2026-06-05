package pl.pb.monopoly.controller.rest;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import pl.pb.monopoly.dto.GameStateDto;
import pl.pb.monopoly.dto.SellPropertyRequest;
import pl.pb.monopoly.dto.TransferRequest;
import pl.pb.monopoly.service.GameService;

import java.util.Map;

/**
 * REST API rozgrywki:
 *  - GET  /api/game/{id}/state    - aktualny stan
 *  - POST /api/game/{id}/roll     - rzut kostka
 *  - POST /api/game/{id}/transfer - przelew siana
 *  - POST /api/game/{id}/buy      - aktywny gracz kupuje pole
 *  - POST /api/game/{id}/skip     - aktywny gracz pomija kupno (zostawia wolne)
 *  - POST /api/game/{id}/bid      - inny gracz licytuje pole (kwota w body)
 */
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
}
