package pl.pb.monopoly.controller.rest;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pl.pb.monopoly.domain.Role;
import pl.pb.monopoly.dto.AdminUserDto;
import pl.pb.monopoly.service.UserService;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserRestController {

    private final UserService userService;

    public AdminUserRestController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public List<AdminUserDto> list(@RequestParam(required = false) String sort,
                                   @RequestParam(required = false) String dir,
                                   @RequestParam(required = false)
                                   @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
                                   @RequestParam(required = false) Role role) {
        return userService.findForAdmin(sort, dir, dateFrom, role).stream()
                .map(u -> new AdminUserDto(
                        u.getId(), u.getUsername(), u.getEmail(),
                        u.getRole().name(), u.getCoins(), u.getCreatedAt(), u.isVerified()))
                .toList();
    }
}
