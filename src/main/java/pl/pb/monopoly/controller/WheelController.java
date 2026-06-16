package pl.pb.monopoly.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import pl.pb.monopoly.dto.WheelResultDto;
import pl.pb.monopoly.service.WheelService;

@Controller
public class WheelController {

    private final WheelService wheelService;

    public WheelController(WheelService wheelService) {
        this.wheelService = wheelService;
    }

    @GetMapping("/wheel")
    public String wheelPage(Authentication authentication, Model model) {
        model.addAttribute("segments", WheelService.rewardLabels());
        model.addAttribute("canSpin", wheelService.canSpinToday(authentication.getName()));
        return "wheel";
    }

    @PostMapping("/wheel/spin")
    @ResponseBody
    public ResponseEntity<WheelResultDto> spin(Authentication authentication) {
        return ResponseEntity.ok(wheelService.spin(authentication.getName()));
    }
}
