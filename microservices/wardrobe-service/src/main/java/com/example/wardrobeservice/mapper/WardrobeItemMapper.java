package com.example.wardrobeservice.mapper;

import com.example.wardrobeservice.dto.WardrobeItemDto;
import com.example.wardrobeservice.dto.WardrobeItemResponseDto;
import com.example.wardrobeservice.entity.WardrobeItem;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface WardrobeItemMapper {

    WardrobeItemResponseDto toDto(WardrobeItem item);

    @Mapping(target = "id", ignore = true)
    WardrobeItem toEntity(WardrobeItemDto dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    void updateEntityFromDto(WardrobeItemDto dto, @MappingTarget WardrobeItem item);
}
