package pl.pb.monopoly.dto;

public record HandCardDto(
        String type,
        String label,
        String description,
        String iconClass
) {}
