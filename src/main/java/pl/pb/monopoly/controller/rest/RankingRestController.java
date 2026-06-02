package pl.pb.monopoly.controller.rest;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.pb.monopoly.dto.RankingEntryDto;
import pl.pb.monopoly.service.UserService;

import java.util.List;

/**
 * Usluga REST (wymaganie: usluga REST). Publiczny endpoint zwracajacy JSON
 * z globalnym rankingiem najlepszych graczy wg punktow ELO (jak FACEIT).
 *
 * Przyklad: GET http://localhost:8080/api/ranking-najlepszych
 */
@RestController
@RequestMapping("/api")
public class RankingRestController {

    private final UserService userService;

    public RankingRestController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/ranking-najlepszych")
    public List<RankingEntryDto> rankingNajlepszych() {
        return userService.topPlayers();
    }
}
