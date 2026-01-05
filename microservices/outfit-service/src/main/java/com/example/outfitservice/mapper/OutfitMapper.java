package com.example.outfitservice.mapper;

import com.example.outfitservice.dto.OutfitDto;
import com.example.outfitservice.dto.OutfitItemLinkDto;
import com.example.outfitservice.dto.OutfitResponseDto;
import com.example.outfitservice.entity.Outfit;
import com.example.outfitservice.entity.OutfitItem;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
public class OutfitMapper {

    public OutfitResponseDto toDto(Outfit outfit) {
        if (outfit == null) return null;

        List<OutfitItemLinkDto> items = outfit.getOutfitItems() == null
                ? List.of()
                : outfit.getOutfitItems().stream()
                .sorted(
                        Comparator.comparingInt(OutfitItem::getPositionIndex)
                                .thenComparing(OutfitItem::getItemId, Comparator.nullsLast(Long::compareTo))
                )
                .map(it -> new OutfitItemLinkDto(it.getItemId(), it.getRole()))
                .toList();

        return new OutfitResponseDto(
                outfit.getId(),
                outfit.getTitle(),
                outfit.getUserId(),
                items
        );
    }

    public Outfit toEntity(OutfitDto dto) {
        if (dto == null) return null;
        Outfit outfit = new Outfit();
        outfit.setTitle(dto.title());
        outfit.setUserId(dto.userId());
        return outfit;
    }

    /**
     * Обновляем только те поля, которые пришли не null.
     * items намеренно не трогаем: ими управляет сервис (чтобы корректно проставлять связи).
     */
    public void updateEntityFromDto(OutfitDto dto, Outfit outfit) {
        if (dto == null || outfit == null) return;
        if (dto.title() != null) outfit.setTitle(dto.title());
        if (dto.userId() != null) outfit.setUserId(dto.userId());
    }
}
