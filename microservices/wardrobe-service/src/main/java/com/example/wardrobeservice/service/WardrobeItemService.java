package com.example.wardrobeservice.service;

import com.example.wardrobeservice.dto.WardrobeItemDto;
import com.example.wardrobeservice.dto.WardrobeItemResponseDto;
import com.example.wardrobeservice.entity.WardrobeItem;
import com.example.wardrobeservice.exception.NotFoundException;
import com.example.wardrobeservice.mapper.WardrobeItemMapper;
import com.example.wardrobeservice.repository.WardrobeItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class WardrobeItemService {

    private final WardrobeItemRepository itemRepository;
    private final WardrobeItemMapper itemMapper;
    private final UserServiceClientWrapper userServiceClientWrapper;

    public Mono<PagedResult<WardrobeItemResponseDto>> getItemsUpTo50(int page, int size) {
        int limit = Math.min(size, 50);
        int offset = page * limit;

        Mono<Long> countMono = itemRepository.countAll();
        Flux<WardrobeItem> itemsFlux = itemRepository.findAllWithPagination(limit, offset);

        return Mono.zip(
                itemsFlux.map(itemMapper::toDto).collectList(),
                countMono
        ).map(tuple -> new PagedResult<>(tuple.getT1(), tuple.getT2()));
    }

    public Flux<WardrobeItemResponseDto> getInfiniteScroll(int offset, int limit) {
        int actualLimit = Math.min(limit, 50);
        return itemRepository.findAllWithPagination(actualLimit, offset)
                .map(itemMapper::toDto);
    }

    public Mono<WardrobeItemResponseDto> getById(Long id) {
        return itemRepository.findById(id)
                .switchIfEmpty(Mono.error(new NotFoundException("Wardrobe item not found with id: " + id)))
                .map(itemMapper::toDto);
    }

    public Mono<WardrobeItemResponseDto> create(WardrobeItemDto dto) {
        // Проверка существования пользователя через Wrapper с Circuit Breaker
        return userServiceClientWrapper.getUserById(dto.ownerId())
                .flatMap(user -> {
                    WardrobeItem item = itemMapper.toEntity(dto);
                    item.setCreatedAt(Instant.now());
                    return itemRepository.save(item)
                            .map(itemMapper::toDto);
                });
    }

    public Mono<WardrobeItemResponseDto> update(Long id, WardrobeItemDto dto) {
        return itemRepository.findById(id)
                .switchIfEmpty(Mono.error(new NotFoundException("Wardrobe item not found with id: " + id)))
                .flatMap(existingItem -> {
                    // Проверка существования пользователя через Wrapper с Circuit Breaker
                    return userServiceClientWrapper.getUserById(dto.ownerId())
                            .flatMap(user -> {
                                itemMapper.updateEntityFromDto(dto, existingItem);
                                return itemRepository.save(existingItem)
                                        .map(itemMapper::toDto);
                            });
                });
    }

    public Mono<Void> delete(Long id) {
        return itemRepository.existsById(id)
                .flatMap(exists -> {
                    if (!exists) {
                        return Mono.error(new NotFoundException("Wardrobe item not found with id: " + id));
                    }
                    return itemRepository.deleteById(id);
                });
    }
}
