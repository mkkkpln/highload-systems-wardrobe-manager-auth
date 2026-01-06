package com.example.wardrobeservice.mapper;

import com.example.wardrobeservice.dto.WardrobeItemDto;
import com.example.wardrobeservice.dto.WardrobeItemResponseDto;
import com.example.wardrobeservice.entity.WardrobeItem;
import org.springframework.stereotype.Component;

@Component
public class WardrobeItemMapperManualImpl implements WardrobeItemMapper {

    @Override
    public WardrobeItemResponseDto toDto(WardrobeItem item) {
        if (item == null) return null;
        return new WardrobeItemResponseDto(
                item.getId(),
                item.getType(),
                item.getBrand(),
                item.getColor(),
                item.getSeason(),
                item.getImageUrl(),
                item.getOwnerId()
        );
    }

    @Override
    public WardrobeItem toEntity(WardrobeItemDto dto) {
        if (dto == null) return null;
        return WardrobeItem.builder()
                .type(dto.type())
                .brand(dto.brand())
                .color(dto.color())
                .season(dto.season())
                .imageUrl(dto.imageUrl())
                .ownerId(dto.ownerId())
                .build();
    }

    @Override
    public void updateEntityFromDto(WardrobeItemDto dto, WardrobeItem item) {
        if (dto == null || item == null) return;
        if (dto.type() != null) item.setType(dto.type());
        if (dto.brand() != null) item.setBrand(dto.brand());
        if (dto.color() != null) item.setColor(dto.color());
        if (dto.season() != null) item.setSeason(dto.season());
        if (dto.imageUrl() != null) item.setImageUrl(dto.imageUrl());
        if (dto.ownerId() != null) item.setOwnerId(dto.ownerId());
    }
}


