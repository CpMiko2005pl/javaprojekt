package pl.pb.monopoly.controller.rest;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import pl.pb.monopoly.dto.GameStateDto;
import pl.pb.monopoly.dto.TransferRequest;
import pl.pb.monopoly.service.GameService;

import java.util.Map;

/**
 * REST API rozgrywki dla planszy na canvasie:
 *  - GET  /api/game/{id}/state    - aktualny stan (gracze, pozycje, siano)
 *  - POST /api/game/{id}/roll     - rzut kostka i ruch aktualnego gracza
 *  - POST /api/game/{id}/transfer - przelej siano do innego gracza
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
}
