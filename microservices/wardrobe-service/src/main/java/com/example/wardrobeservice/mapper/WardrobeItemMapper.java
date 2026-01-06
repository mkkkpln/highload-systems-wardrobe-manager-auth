package com.example.wardrobeservice.mapper;

import com.example.wardrobeservice.dto.WardrobeItemDto;
import com.example.wardrobeservice.dto.WardrobeItemResponseDto;
import com.example.wardrobeservice.entity.WardrobeItem;

public interface WardrobeItemMapper {

    WardrobeItemResponseDto toDto(WardrobeItem item);

    WardrobeItem toEntity(WardrobeItemDto dto);

    void updateEntityFromDto(WardrobeItemDto dto, WardrobeItem item);
}
