package pl.pb.monopoly.dto;

import java.time.LocalDateTime;

/** Lekki widok uzytkownika dla uslugi REST admina (GET /api/admin/users). */
public record AdminUserDto(
        Long id,
        String username,
        String email,
        String role,
        int coins,
        LocalDateTime createdAt,
        boolean verified
) {}
