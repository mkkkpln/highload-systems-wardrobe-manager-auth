package com.example.wardrobeservice.service;

import com.example.wardrobeservice.dto.WardrobeItemDto;
import com.example.wardrobeservice.dto.WardrobeItemResponseDto;
import com.example.wardrobeservice.entity.WardrobeItem;
import com.example.wardrobeservice.exception.NotFoundException;
import com.example.wardrobeservice.mapper.WardrobeItemMapper;
import com.example.wardrobeservice.repository.WardrobeItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Collection;

@Service
@RequiredArgsConstructor
public class WardrobeItemService {

    private final WardrobeItemRepository itemRepository;
    private final WardrobeItemMapper itemMapper;
    private final UserServiceClientWrapper userServiceClientWrapper;

    public Mono<PagedResult<WardrobeItemResponseDto>> getItemsUpTo50(int page, int size) {
        int limit = Math.min(size, 50);
        int offset = page * limit;

        return currentJwt().flatMap(jwt -> {
            if (isSupervisor(jwt)) {
                Mono<Long> countMono = itemRepository.countAll();
                Flux<WardrobeItem> itemsFlux = itemRepository.findAllWithPagination(limit, offset);
                return Mono.zip(itemsFlux.map(itemMapper::toDto).collectList(), countMono)
                        .map(tuple -> new PagedResult<>(tuple.getT1(), tuple.getT2()));
            }

            Long userId = requireUserId(jwt);
            Mono<Long> countMono = itemRepository.countByOwnerId(userId);
            Flux<WardrobeItem> itemsFlux = itemRepository.findAllByOwnerIdWithPagination(userId, limit, offset);
            return Mono.zip(itemsFlux.map(itemMapper::toDto).collectList(), countMono)
                    .map(tuple -> new PagedResult<>(tuple.getT1(), tuple.getT2()));
        });
    }

    public Flux<WardrobeItemResponseDto> getInfiniteScroll(int offset, int limit) {
        int actualLimit = Math.min(limit, 50);
        return currentJwt().flatMapMany(jwt -> {
            if (isSupervisor(jwt)) {
                return itemRepository.findAllWithPagination(actualLimit, offset).map(itemMapper::toDto);
            }
            Long userId = requireUserId(jwt);
            return itemRepository.findAllByOwnerIdWithPagination(userId, actualLimit, offset).map(itemMapper::toDto);
        });
    }

    public Mono<WardrobeItemResponseDto> getById(Long id) {
        return currentJwt().flatMap(jwt ->
                itemRepository.findById(id)
                        .switchIfEmpty(Mono.error(new NotFoundException("Wardrobe item not found with id: " + id)))
                        .flatMap(item -> {
                            if (isSupervisor(jwt)) return Mono.just(itemMapper.toDto(item));
                            Long userId = requireUserId(jwt);
                            if (item.getOwnerId() == null || !item.getOwnerId().equals(userId)) {
                                return Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied"));
                            }
                            return Mono.just(itemMapper.toDto(item));
                        })
        );
    }

    public Mono<WardrobeItemResponseDto> create(WardrobeItemDto dto) {
        return currentJwt().flatMap(jwt -> {
            if (!isSupervisor(jwt)) {
                Long userId = requireUserId(jwt);
                if (dto.ownerId() == null || !dto.ownerId().equals(userId)) {
                    return Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN, "User can create items only for self"));
                }
            }

            // Проверка существования пользователя через Wrapper с Circuit Breaker
            return userServiceClientWrapper.getUserById(dto.ownerId())
                    .flatMap(user -> {
                        WardrobeItem item = itemMapper.toEntity(dto);
                        item.setCreatedAt(Instant.now());
                        return itemRepository.save(item).map(itemMapper::toDto);
                    });
        });
    }

    public Mono<WardrobeItemResponseDto> update(Long id, WardrobeItemDto dto) {
        return currentJwt().flatMap(jwt ->
                itemRepository.findById(id)
                        .switchIfEmpty(Mono.error(new NotFoundException("Wardrobe item not found with id: " + id)))
                        .flatMap(existingItem -> {
                            if (!isSupervisor(jwt)) {
                                Long userId = requireUserId(jwt);
                                if (existingItem.getOwnerId() == null || !existingItem.getOwnerId().equals(userId)) {
                                    return Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied"));
                                }
                                if (dto.ownerId() == null || !dto.ownerId().equals(userId)) {
                                    return Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN, "User can update items only for self"));
                                }
                            }

                            // Проверка существования пользователя через Wrapper с Circuit Breaker
                            return userServiceClientWrapper.getUserById(dto.ownerId())
                                    .flatMap(user -> {
                                        itemMapper.updateEntityFromDto(dto, existingItem);
                                        return itemRepository.save(existingItem).map(itemMapper::toDto);
                                    });
                        })
        );
    }

    public Mono<Void> delete(Long id) {
        return currentJwt().flatMap(jwt -> {
            if (isSupervisor(jwt)) {
                return itemRepository.existsById(id)
                        .flatMap(exists -> {
                            if (!exists) {
                                return Mono.error(new NotFoundException("Wardrobe item not found with id: " + id));
                            }
                            return itemRepository.deleteById(id);
                        });
            }

            Long userId = requireUserId(jwt);
            return itemRepository.findById(id)
                    .switchIfEmpty(Mono.error(new NotFoundException("Wardrobe item not found with id: " + id)))
                    .flatMap(item -> {
                        if (item.getOwnerId() == null || !item.getOwnerId().equals(userId)) {
                            return Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied"));
                        }
                        return itemRepository.deleteById(id);
                    });
        });
    }

    private static Mono<Jwt> currentJwt() {
        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> ctx.getAuthentication())
                .cast(JwtAuthenticationToken.class)
                .map(JwtAuthenticationToken::getToken)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized")));
    }

    private static Long requireUserId(Jwt jwt) {
        String userId = jwt.getClaimAsString("userId");
        if (userId == null || userId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing userId claim");
        }
        try {
            return Long.parseLong(userId);
        } catch (NumberFormatException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid userId claim");
        }
    }

    private static boolean isSupervisor(Jwt jwt) {
        Object roles = jwt.getClaims().get("roles");
        if (roles instanceof Collection<?> c) {
            return c.stream().anyMatch(r -> "ROLE_SUPERVISOR".equals(String.valueOf(r)) || "ROLE_ADMIN".equals(String.valueOf(r)));
        }
        String str = roles == null ? "" : roles.toString();
        return str.contains("ROLE_SUPERVISOR") || str.contains("ROLE_ADMIN");
    }
}
