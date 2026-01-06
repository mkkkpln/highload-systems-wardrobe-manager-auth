package com.example.outfitservice.service;

import com.example.outfitservice.dto.OutfitDto;
import com.example.outfitservice.dto.OutfitResponseDto;
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
        Pageable pageable = PageRequest.of(offset / Math.min(limit, 50), Math.min(limit, 50));
        Jwt jwt = currentJwt();
        Page<Outfit> pageResult = isSupervisor(jwt)
                ? outfitRepository.findAll(pageable)
                : outfitRepository.findAllByUserId(requireUserId(jwt), pageable);

        return pageResult.getContent()
                .stream()
                .map(outfitMapper::toDto)
                .toList();
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
            return c.stream().anyMatch(r -> "ROLE_SUPERVISOR".equals(String.valueOf(r)) || "ROLE_ADMIN".equals(String.valueOf(r)));
        }
        String str = roles == null ? "" : roles.toString();
        return str.contains("ROLE_SUPERVISOR") || str.contains("ROLE_ADMIN");
    }
}
