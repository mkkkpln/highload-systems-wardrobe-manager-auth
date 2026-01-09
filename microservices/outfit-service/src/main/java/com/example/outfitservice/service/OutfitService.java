package com.example.outfitservice.service;

import com.example.outfitservice.dto.OutfitDto;
import com.example.outfitservice.dto.OutfitDetailedResponseDto;
import com.example.outfitservice.dto.OutfitResponseDto;
import com.example.outfitservice.dto.OutfitItemDetailedDto;
import com.example.outfitservice.dto.OutfitItemLinkDto;
import com.example.outfitservice.entity.Outfit;
import com.example.outfitservice.entity.OutfitItem;
import com.example.outfitservice.exception.NotFoundException;
import com.example.outfitservice.mapper.OutfitMapper;
import com.example.outfitservice.repository.OutfitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OutfitService {

    private final OutfitRepository outfitRepository;
    private final OutfitMapper outfitMapper;
    private final UserServiceClientWrapper userServiceClientWrapper;
    private final WardrobeServiceClientWrapper wardrobeServiceClientWrapper;

    public PagedResult<OutfitResponseDto> getOutfitsUpTo50(int page, int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 50));
        Jwt jwt = currentJwt();
        Page<Outfit> outfitsPage = isSupervisor(jwt)
                ? outfitRepository.findAll(pageable)
                : outfitRepository.findAllByUserId(requireUserId(jwt), pageable);

        List<OutfitResponseDto> content = outfitsPage.getContent().stream()
                .map(outfitMapper::toDto)
                .toList();

        return new PagedResult<>(content, outfitsPage.getTotalElements());
    }

    public List<OutfitResponseDto> getInfiniteScroll(int offset, int limit) {
        Jwt jwt = currentJwt();
        int actualLimit = Math.min(limit, 50);
        long fromId = offset;

        List<Outfit> outfits = isSupervisor(jwt)
                ? outfitRepository.findAllScrollFromId(fromId, actualLimit)
                : outfitRepository.findAllByUserIdScrollFromId(requireUserId(jwt), fromId, actualLimit);

        return outfits.stream().map(outfitMapper::toDto).toList();
    }

    public OutfitResponseDto getById(Long id) {
        Jwt jwt = currentJwt();
        if (!isSupervisor(jwt)) {
            Long userId = requireUserId(jwt);
            if (!outfitRepository.existsByIdAndUserId(id, userId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
            }
        }
        Outfit outfit = outfitRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Outfit not found with id: " + id));
        return outfitMapper.toDto(outfit);
    }

    /**
     * Same as {@link #getById(Long)} but also enriches each item link with full wardrobe item details.
     * Used by REST endpoint GET /outfits/{id} to "show items подробно" by default.
     */
    public OutfitResponseDto getByIdWithItemDetails(Long id) {
        OutfitResponseDto basic = getById(id);
        Jwt jwt = currentJwt();
        String bearer = "Bearer " + jwt.getTokenValue();

        List<OutfitItemLinkDto> enrichedItems = (basic.items() == null ? List.<OutfitItemLinkDto>of() : basic.items())
                .stream()
                .map(link -> {
                    try {
                        var item = wardrobeServiceClientWrapper.getItemById(bearer, link.itemId());
                        return new OutfitItemLinkDto(link.itemId(), link.role(), item);
                    } catch (ResponseStatusException ex) {
                        // If item is missing/not accessible OR wardrobe-service is temporarily unavailable,
                        // keep basic link so outfit still renders (do not fail whole GET /outfits/{id}).
                        if (ex.getStatusCode().value() == 404
                                || ex.getStatusCode().value() == 403
                                || ex.getStatusCode().value() == 503) {
                            return new OutfitItemLinkDto(link.itemId(), link.role(), null);
                        }
                        throw ex;
                    }
                })
                .toList();

        return new OutfitResponseDto(basic.id(), basic.title(), basic.userId(), enrichedItems);
    }

    public OutfitDetailedResponseDto getDetailedById(Long id) {
        // reuse access rules from getById
        OutfitResponseDto basic = getById(id);
        return toDetailed(basic);
    }

    public List<OutfitResponseDto> getMyOutfits() {
        Jwt jwt = currentJwt();
        Long userId = requireUserId(jwt);
        return outfitRepository.findAllByUserId(userId).stream()
                .map(outfitMapper::toDto)
                .toList();
    }

    public List<OutfitDetailedResponseDto> getMyOutfitsDetailed() {
        return getMyOutfits().stream().map(this::toDetailed).toList();
    }

    @Transactional
    public OutfitResponseDto create(OutfitDto dto) {
        Jwt jwt = currentJwt();
        if (!isSupervisor(jwt)) {
            Long userId = requireUserId(jwt);
            if (dto.userId() == null || !dto.userId().equals(userId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User can create outfits only for self");
            }
        }

        String authorization = "Bearer " + jwt.getTokenValue();
        userServiceClientWrapper.getUserById(authorization, dto.userId());

        Outfit outfit = outfitMapper.toEntity(dto);
        applyItemsFromDto(dto, outfit);
        return outfitMapper.toDto(outfitRepository.save(outfit));
    }

    @Transactional
    public OutfitResponseDto update(Long id, OutfitDto dto) {
        Outfit outfit = outfitRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Outfit not found with id: " + id));

        Jwt jwt = currentJwt();
        if (!isSupervisor(jwt)) {
            Long userId = requireUserId(jwt);
            if (outfit.getUserId() == null || !outfit.getUserId().equals(userId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
            }
            if (dto.userId() == null || !dto.userId().equals(userId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User can update outfits only for self");
            }
        }

        String authorization = "Bearer " + jwt.getTokenValue();
        userServiceClientWrapper.getUserById(authorization, dto.userId());

        outfitMapper.updateEntityFromDto(dto, outfit);
        applyItemsFromDto(dto, outfit);
        return outfitMapper.toDto(outfitRepository.save(outfit));
    }

    @Transactional
    public OutfitDetailedResponseDto updateDetailed(Long id, OutfitDto dto) {
        OutfitResponseDto updated = update(id, dto);
        return toDetailed(updated);
    }

    @Transactional
    public void delete(Long id) {
        Jwt jwt = currentJwt();
        if (!isSupervisor(jwt)) {
            Long userId = requireUserId(jwt);
            if (!outfitRepository.existsByIdAndUserId(id, userId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
            }
        }

        if (!outfitRepository.existsById(id)) {
            throw new NotFoundException("Outfit not found with id: " + id);
        }
        outfitRepository.deleteById(id);
    }

    /**
     * items обновляются только если поле dto.items != null.
     * Если items = [], то очищаем связи.
     */
    private void applyItemsFromDto(OutfitDto dto, Outfit outfit) {
        if (dto.items() == null) return;

        if (outfit == null) return;

        // Чтобы не ловить DuplicateKey в Hibernate при обновлении (когда удаляем и добавляем сущность с тем же PK),
        // обновляем существующие OutfitItem "на месте", удаляем только лишние, и добавляем только реально новые.
        Map<Long, OutfitItem> existingByItemId = new HashMap<>();
        for (OutfitItem existing : outfit.getOutfitItems()) {
            if (existing.getItemId() != null) {
                existingByItemId.put(existing.getItemId(), existing);
            }
        }

        Set<Long> desiredItemIds = dto.items().stream()
                .map(link -> link.itemId())
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        outfit.getOutfitItems().removeIf(existing -> existing.getItemId() == null || !desiredItemIds.contains(existing.getItemId()));

        for (int i = 0; i < dto.items().size(); i++) {
            var link = dto.items().get(i);
            OutfitItem oi = existingByItemId.get(link.itemId());
            if (oi == null) {
                oi = new OutfitItem();
                oi.setOutfit(outfit);
                oi.setItemId(link.itemId());
                outfit.getOutfitItems().add(oi);
            }
            oi.setRole(link.role());
            oi.setPositionIndex(i + 1);
        }
    }

    private static Jwt currentJwt() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (!(auth instanceof JwtAuthenticationToken jwtAuth)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        return jwtAuth.getToken();
    }

    private String bearer(Jwt jwt) {
        return "Bearer " + jwt.getTokenValue();
    }

    private OutfitDetailedResponseDto toDetailed(OutfitResponseDto basic) {
        Jwt jwt = currentJwt();
        String authorization = bearer(jwt);

        List<OutfitItemDetailedDto> detailedItems = (basic.items() == null ? List.<OutfitItemLinkDto>of() : basic.items())
                .stream()
                .map(link -> {
                    var item = wardrobeServiceClientWrapper.getItemById(authorization, link.itemId());
                    return new OutfitItemDetailedDto(link.itemId(), link.role(), item);
                })
                .toList();

        return new OutfitDetailedResponseDto(
                basic.id(),
                basic.title(),
                basic.userId(),
                detailedItems
        );
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
        if (roles instanceof java.util.Collection<?> c) {
            return c.stream().anyMatch(r ->
                    "ROLE_SUPERVISOR".equals(String.valueOf(r))
                            || "ROLE_MODERATOR".equals(String.valueOf(r))
                            || "ROLE_ADMIN".equals(String.valueOf(r))
            );
        }
        String str = roles == null ? "" : roles.toString();
        return str.contains("ROLE_SUPERVISOR") || str.contains("ROLE_MODERATOR") || str.contains("ROLE_ADMIN");
    }
}
