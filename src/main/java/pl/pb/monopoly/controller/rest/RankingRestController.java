package pl.pb.monopoly.controller.rest;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.pb.monopoly.dto.RankingEntryDto;
import pl.pb.monopoly.service.UserService;

import java.util.List;

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
