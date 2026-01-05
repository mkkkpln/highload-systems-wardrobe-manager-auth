package com.example.outfitservice.dto;

import com.example.outfitservice.entity.OutfitRole;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.NotNull;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record OutfitItemLinkDto(
        @NotNull
        Long itemId,

        @NotNull
        OutfitRole role
) {
}
