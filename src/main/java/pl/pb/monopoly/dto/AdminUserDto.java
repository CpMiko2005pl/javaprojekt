package pl.pb.monopoly.dto;

import java.time.LocalDateTime;

public record AdminUserDto(
        Long id,
        String username,
        String email,
        String role,
        int coins,
        LocalDateTime createdAt,
        boolean verified
) {}
