package com.example.outfitservice.service;

import com.example.outfitservice.dto.OutfitDto;
import com.example.outfitservice.dto.OutfitItemLinkDto;
import com.example.outfitservice.dto.OutfitResponseDto;
import com.example.outfitservice.dto.UserDto;
import com.example.outfitservice.entity.Outfit;
import com.example.outfitservice.entity.OutfitRole;
import com.example.outfitservice.exception.NotFoundException;
import com.example.outfitservice.mapper.OutfitMapper;
import com.example.outfitservice.repository.OutfitRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutfitServiceTest {

    @Mock
    private OutfitRepository outfitRepository;

    @Mock
    private OutfitMapper outfitMapper;

    @Mock
    private UserServiceClientWrapper userServiceClientWrapper;

    @InjectMocks
    private OutfitService outfitService;

    private Outfit testOutfit;
    private OutfitResponseDto testOutfitDto;
    private UserDto testUser;

    @BeforeEach
    void setUp() {
        testOutfit = new Outfit();
        testOutfit.setId(1L);
        testOutfit.setTitle("Summer Outfit");
        testOutfit.setUserId(1L);
        testOutfit.setCreatedAt(Instant.now());

        testOutfitDto = new OutfitResponseDto(
                1L,
                "Summer Outfit",
                1L,
                List.of(new OutfitItemLinkDto(1L, OutfitRole.TOP))
        );

        testUser = new UserDto(1L, "user@example.com", "Test User");
    }

    @Test
    void getOutfitsUpTo50_shouldReturnPagedResult() {
        // Given
        int page = 0;
        int size = 10;
        Pageable pageable = PageRequest.of(page, size);
        Page<Outfit> outfitPage = new PageImpl<>(List.of(testOutfit), pageable, 1);

        when(outfitRepository.findAll(pageable)).thenReturn(outfitPage);
        when(outfitMapper.toDto(testOutfit)).thenReturn(testOutfitDto);

        // When
        PagedResult<OutfitResponseDto> result = outfitService.getOutfitsUpTo50(page, size);

        // Then
        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0)).isEqualTo(testOutfitDto);
        assertThat(result.totalCount()).isEqualTo(1);

        verify(outfitRepository).findAll(pageable);
        verify(outfitMapper).toDto(testOutfit);
    }

    @Test
    void getOutfitsUpTo50_shouldLimitSizeTo50() {
        // Given
        int page = 0;
        int size = 100;
        Pageable pageable = PageRequest.of(page, 50);
        Page<Outfit> outfitPage = new PageImpl<>(List.of(testOutfit), pageable, 1);

        when(outfitRepository.findAll(pageable)).thenReturn(outfitPage);
        when(outfitMapper.toDto(testOutfit)).thenReturn(testOutfitDto);

        // When
        PagedResult<OutfitResponseDto> result = outfitService.getOutfitsUpTo50(page, size);

        // Then
        verify(outfitRepository).findAll(PageRequest.of(page, 50));
    }

    @Test
    void getInfiniteScroll_shouldReturnList() {
        // Given
        int offset = 0;
        int limit = 10;
        Pageable pageable = PageRequest.of(0, 10);
        Page<Outfit> outfitPage = new PageImpl<>(List.of(testOutfit), pageable, 1);

        when(outfitRepository.findAll(pageable)).thenReturn(outfitPage);
        when(outfitMapper.toDto(testOutfit)).thenReturn(testOutfitDto);

        // When
        List<OutfitResponseDto> result = outfitService.getInfiniteScroll(offset, limit);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(testOutfitDto);

        verify(outfitRepository).findAll(pageable);
        verify(outfitMapper).toDto(testOutfit);
    }

    @Test
    void getInfiniteScroll_shouldLimitTo50() {
        // Given
        int offset = 0;
        int limit = 100;
        Pageable pageable = PageRequest.of(0, 50);
        Page<Outfit> outfitPage = new PageImpl<>(List.of(testOutfit), pageable, 1);

        when(outfitRepository.findAll(pageable)).thenReturn(outfitPage);
        when(outfitMapper.toDto(testOutfit)).thenReturn(testOutfitDto);

        // When
        List<OutfitResponseDto> result = outfitService.getInfiniteScroll(offset, limit);

        // Then
        verify(outfitRepository).findAll(PageRequest.of(0, 50));
    }

    @Test
    void getById_shouldReturnOutfit() {
        // Given
        Long id = 1L;
        when(outfitRepository.findById(id)).thenReturn(Optional.of(testOutfit));
        when(outfitMapper.toDto(testOutfit)).thenReturn(testOutfitDto);

        // When
        OutfitResponseDto result = outfitService.getById(id);

        // Then
        assertThat(result).isEqualTo(testOutfitDto);

        verify(outfitRepository).findById(id);
        verify(outfitMapper).toDto(testOutfit);
    }

    @Test
    void getById_shouldThrowNotFoundException_whenOutfitNotExists() {
        // Given
        Long id = 999L;
        when(outfitRepository.findById(id)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> outfitService.getById(id))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Outfit not found with id: " + id);

        verify(outfitRepository).findById(id);
        verify(outfitMapper, never()).toDto(any());
    }

//    @Test
//    void create_shouldCreateOutfit() {
//        // Given
//        OutfitDto createDto = new OutfitDto( "New Outfit", 1L,
//                List.of(new OutfitItemLinkDto(1L, OutfitRole.TOP)));
//        Outfit savedOutfit = new Outfit();
//        savedOutfit.setId(2L);
//        savedOutfit.setTitle("New Outfit");
//        savedOutfit.setUserId(1L);
//
//        when(userServiceClientWrapper.getUserById(1L)).thenReturn(testUser);
//        when(outfitMapper.toEntity(createDto)).thenReturn(savedOutfit);
//        when(outfitRepository.save(any(Outfit.class))).thenReturn(savedOutfit);
//        when(outfitMapper.toDto(savedOutfit)).thenReturn(createDto);
//
//        // When
//        OutfitResponseDto result = outfitService.create(createDto);
//
//        // Then
//        assertThat(result).isEqualTo(createDto);
//
//        verify(userServiceClientWrapper).getUserById(1L);
//        verify(outfitMapper).toEntity(createDto);
//        verify(outfitRepository).save(any(Outfit.class));
//        verify(outfitMapper).toDto(savedOutfit);
//    }

//    @Test
//    void update_shouldUpdateOutfit() {
//        // Given
//        Long id = 1L;
//        OutfitResponseDto updateDto = new OutfitResponseDto(1L, "Updated Outfit", 1L,
//                List.of(new OutfitItemLinkDto(1L, OutfitRole.TOP)));
//
//        when(outfitRepository.findById(id)).thenReturn(Optional.of(testOutfit));
//        when(userServiceClientWrapper.getUserById(1L)).thenReturn(testUser);
//        when(outfitRepository.save(any(Outfit.class))).thenReturn(testOutfit);
//        when(outfitMapper.toDto(testOutfit)).thenReturn(updateDto);
//
//        // When
//        OutfitResponseDto result = outfitService.update(id, updateDto);
//
//        // Then
//        assertThat(result).isEqualTo(updateDto);
//
//        verify(outfitRepository).findById(id);
//        verify(userServiceClientWrapper).getUserById(1L);
//        verify(outfitRepository).save(testOutfit);
//    }

    @Test
    void update_shouldThrowNotFoundException_whenOutfitNotExists() {
        // Given
        Long id = 999L;
        OutfitDto updateDto = new OutfitDto("Updated Outfit", 1L,
                List.of(new OutfitItemLinkDto(1L, OutfitRole.TOP)));

        when(outfitRepository.findById(id)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> outfitService.update(id, updateDto))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Outfit not found with id: " + id);

        verify(outfitRepository).findById(id);
        verify(userServiceClientWrapper, never()).getUserById(anyLong());
    }

    @Test
    void update_shouldPropagateError_whenUserServiceFails() {
        // Given
        Long id = 1L;
        OutfitDto updateDto = new OutfitDto("Updated Outfit", 999L,
                List.of(new OutfitItemLinkDto(1L, OutfitRole.TOP)));

        when(outfitRepository.findById(id)).thenReturn(Optional.of(testOutfit));
        when(userServiceClientWrapper.getUserById(999L)).thenThrow(new RuntimeException("User service failed"));

        // When & Then
        assertThatThrownBy(() -> outfitService.update(id, updateDto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User service failed");

        verify(outfitRepository).findById(id);
        verify(userServiceClientWrapper).getUserById(999L);
    }

    @Test
    void delete_shouldDeleteOutfit() {
        // Given
        Long id = 1L;
        when(outfitRepository.existsById(id)).thenReturn(true);

        // When
        outfitService.delete(id);

        // Then
        verify(outfitRepository).existsById(id);
        verify(outfitRepository).deleteById(id);
    }

    @Test
    void delete_shouldThrowNotFoundException_whenOutfitNotExists() {
        // Given
        Long id = 999L;
        when(outfitRepository.existsById(id)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> outfitService.delete(id))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Outfit not found with id: " + id);

        verify(outfitRepository).existsById(id);
        verify(outfitRepository, never()).deleteById(anyLong());
    }
}
