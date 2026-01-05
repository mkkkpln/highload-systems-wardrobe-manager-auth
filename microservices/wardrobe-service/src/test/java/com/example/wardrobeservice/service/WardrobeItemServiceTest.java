package com.example.wardrobeservice.service;

import com.example.wardrobeservice.dto.UserDto;
import com.example.wardrobeservice.dto.WardrobeItemDto;
import com.example.wardrobeservice.dto.WardrobeItemResponseDto;
import com.example.wardrobeservice.entity.WardrobeItem;
import com.example.wardrobeservice.entity.enums.ItemType;
import com.example.wardrobeservice.entity.enums.Season;
import com.example.wardrobeservice.exception.NotFoundException;
import com.example.wardrobeservice.mapper.WardrobeItemMapper;
import com.example.wardrobeservice.repository.WardrobeItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WardrobeItemServiceTest {

    @Mock
    private WardrobeItemRepository itemRepository;

    @Mock
    private WardrobeItemMapper itemMapper;

    @Mock
    private UserServiceClientWrapper userServiceClientWrapper;

    @InjectMocks
    private WardrobeItemService wardrobeItemService;

    private WardrobeItem testItem;
    private WardrobeItemResponseDto testItemDto;
    private UserDto testUser;

    @BeforeEach
    void setUp() {
        testItem = WardrobeItem.builder()
                .id(1L)
                .ownerId(1L)
                .type(ItemType.SHIRT)
                .brand("Nike")
                .color("Blue")
                .season(Season.SUMMER)
                .imageUrl("image.jpg")
                .createdAt(Instant.now())
                .build();

        testItemDto = new WardrobeItemResponseDto(
                1L, ItemType.SHIRT, "Nike", "Blue", Season.SUMMER, "image.jpg", 1L
        );

        testUser = new UserDto(1L, "test@example.com", "Test User");
    }

    @Test
    void getItemsUpTo50_shouldReturnPagedResult() {
        // Given
        int page = 0;
        int size = 10;
        int limit = 10;
        int offset = 0;
        List<WardrobeItem> items = List.of(testItem);
        List<WardrobeItemResponseDto> itemDtos = List.of(testItemDto);

        when(itemRepository.findAllWithPagination(limit, offset)).thenReturn(Flux.fromIterable(items));
        when(itemRepository.countAll()).thenReturn(Mono.just(1L));
        when(itemMapper.toDto(any(WardrobeItem.class))).thenReturn(testItemDto);

        // When
        Mono<PagedResult<WardrobeItemResponseDto>> result = wardrobeItemService.getItemsUpTo50(page, size);

        // Then
        StepVerifier.create(result)
                .assertNext(pagedResult -> {
                    assertThat(pagedResult.content()).hasSize(1);
                    assertThat(pagedResult.totalElements()).isEqualTo(1L);
                    assertThat(pagedResult.content().get(0).id()).isEqualTo(1L);
                })
                .verifyComplete();

        verify(itemRepository).findAllWithPagination(10, 0);
        verify(itemRepository).countAll();
    }

    @Test
    void getItemsUpTo50_shouldLimitSizeTo50() {
        // Given
        int page = 0;
        int size = 100; // больше 50
        when(itemRepository.findAllWithPagination(50, 0)).thenReturn(Flux.empty());
        when(itemRepository.countAll()).thenReturn(Mono.just(0L));

        // When
        wardrobeItemService.getItemsUpTo50(page, size).block();

        // Then
        verify(itemRepository).findAllWithPagination(50, 0); // должно быть ограничено до 50
    }

    @Test
    void getInfiniteScroll_shouldReturnFluxOfDtos() {
        // Given
        int offset = 0;
        int limit = 10;
        List<WardrobeItem> items = List.of(testItem);

        when(itemRepository.findAllWithPagination(10, 0)).thenReturn(Flux.fromIterable(items));
        when(itemMapper.toDto(any(WardrobeItem.class))).thenReturn(testItemDto);

        // When
        Flux<WardrobeItemResponseDto> result = wardrobeItemService.getInfiniteScroll(offset, limit);

        // Then
        StepVerifier.create(result)
                .assertNext(dto -> {
                    assertThat(dto.id()).isEqualTo(1L);
                    assertThat(dto.type()).isEqualTo(ItemType.SHIRT);
                })
                .verifyComplete();

        verify(itemRepository).findAllWithPagination(10, 0);
    }

    @Test
    void getInfiniteScroll_shouldLimitSizeTo50() {
        // Given
        int offset = 0;
        int limit = 100; // больше 50
        when(itemRepository.findAllWithPagination(50, 0)).thenReturn(Flux.empty());

        // When
        wardrobeItemService.getInfiniteScroll(offset, limit).blockLast();

        // Then
        verify(itemRepository).findAllWithPagination(50, 0); // должно быть ограничено до 50
    }

    @Test
    void getById_shouldReturnItem_whenExists() {
        // Given
        Long id = 1L;
        when(itemRepository.findById(id)).thenReturn(Mono.just(testItem));
        when(itemMapper.toDto(testItem)).thenReturn(testItemDto);

        // When
        Mono<WardrobeItemResponseDto> result = wardrobeItemService.getById(id);

        // Then
        StepVerifier.create(result)
                .assertNext(dto -> {
                    assertThat(dto.id()).isEqualTo(1L);
                    assertThat(dto.type()).isEqualTo(ItemType.SHIRT);
                })
                .verifyComplete();

        verify(itemRepository).findById(id);
        verify(itemMapper).toDto(testItem);
    }

    @Test
    void getById_shouldThrowNotFoundException_whenNotExists() {
        // Given
        Long id = 999L;
        when(itemRepository.findById(id)).thenReturn(Mono.empty());

        // When
        Mono<WardrobeItemResponseDto> result = wardrobeItemService.getById(id);

        // Then
        StepVerifier.create(result)
                .expectErrorMatches(throwable ->
                        throwable instanceof NotFoundException &&
                        throwable.getMessage().contains("Wardrobe item not found with id: 999"))
                .verify();

        verify(itemRepository).findById(id);
        verify(itemMapper, never()).toDto(any());
    }

    @Test
    void create_shouldCreateItem_whenUserExists() {
        // Given
        WardrobeItemDto createDto = new WardrobeItemDto(
                ItemType.SHIRT, "Nike", "Blue", Season.SUMMER, "image.jpg", 1L
        );
        WardrobeItem newItem = WardrobeItem.builder()
                .ownerId(1L)
                .type(ItemType.SHIRT)
                .brand("Nike")
                .color("Blue")
                .season(Season.SUMMER)
                .imageUrl("image.jpg")
                .build();
        WardrobeItem savedItem = WardrobeItem.builder()
                .id(1L)
                .ownerId(1L)
                .type(ItemType.SHIRT)
                .brand("Nike")
                .color("Blue")
                .season(Season.SUMMER)
                .imageUrl("image.jpg")
                .createdAt(Instant.now())
                .build();

        when(userServiceClientWrapper.getUserById(1L)).thenReturn(Mono.just(testUser));
        when(itemMapper.toEntity(createDto)).thenReturn(newItem);
        when(itemRepository.save(any(WardrobeItem.class))).thenReturn(Mono.just(savedItem));
        when(itemMapper.toDto(savedItem)).thenReturn(testItemDto);

        // When
        Mono<WardrobeItemResponseDto> result = wardrobeItemService.create(createDto);

        // Then
        StepVerifier.create(result)
                .assertNext(dto -> {
                    assertThat(dto.id()).isEqualTo(1L);
                    assertThat(dto.ownerId()).isEqualTo(1L);
                })
                .verifyComplete();

        verify(userServiceClientWrapper).getUserById(1L);
        verify(itemMapper).toEntity(createDto);
        verify(itemRepository).save(any(WardrobeItem.class));
    }

    @Test
    void create_shouldThrowException_whenUserDoesNotExist() {
        // Given
        WardrobeItemDto createDto = new WardrobeItemDto(
                ItemType.SHIRT, "Nike", "Blue", Season.SUMMER, "image.jpg", 999L
        );
        when(userServiceClientWrapper.getUserById(999L)).thenReturn(Mono.error(new IllegalArgumentException("User does not exist: 999")));

        // When
        Mono<WardrobeItemResponseDto> result = wardrobeItemService.create(createDto);

        // Then
        StepVerifier.create(result)
                .expectErrorMatches(throwable ->
                        throwable instanceof IllegalArgumentException &&
                        throwable.getMessage().contains("User does not exist: 999"))
                .verify();

        verify(userServiceClientWrapper).getUserById(999L);
        verify(itemMapper, never()).toEntity(any());
        verify(itemRepository, never()).save(any());
    }

    @Test
    void update_shouldUpdateItem_whenExistsAndUserExists() {
        // Given
        Long id = 1L;
        WardrobeItemDto updateDto = new WardrobeItemDto(
                ItemType.JACKET, "Adidas", "Red", Season.WINTER, "new-image.jpg", 1L
        );
        WardrobeItemResponseDto updatedDto = new WardrobeItemResponseDto(
                1L, ItemType.JACKET, "Adidas", "Red", Season.WINTER, "new-image.jpg", 1L
        );

        when(itemRepository.findById(id)).thenReturn(Mono.just(testItem));
        when(userServiceClientWrapper.getUserById(1L)).thenReturn(Mono.just(testUser));
        when(itemRepository.save(any(WardrobeItem.class))).thenReturn(Mono.just(testItem));
        when(itemMapper.toDto(any(WardrobeItem.class))).thenReturn(updatedDto);

        // When
        Mono<WardrobeItemResponseDto> result = wardrobeItemService.update(id, updateDto);

        // Then
        StepVerifier.create(result)
                .assertNext(dto -> {
                    assertThat(dto.type()).isEqualTo(ItemType.JACKET);
                    assertThat(dto.brand()).isEqualTo("Adidas");
                })
                .verifyComplete();

        verify(itemRepository).findById(id);
        verify(userServiceClientWrapper).getUserById(1L);
        verify(itemMapper).updateEntityFromDto(updateDto, testItem);
        verify(itemRepository).save(testItem);
    }

    @Test
    void update_shouldThrowNotFoundException_whenItemNotExists() {
        // Given
        Long id = 999L;
        WardrobeItemDto updateDto = new WardrobeItemDto(ItemType.SHIRT, "Nike", "Blue", Season.SUMMER, "image.jpg", 1L
        );
        when(itemRepository.findById(id)).thenReturn(Mono.empty());

        // When
        Mono<WardrobeItemResponseDto> result = wardrobeItemService.update(id, updateDto);

        // Then
        StepVerifier.create(result)
                .expectErrorMatches(throwable ->
                        throwable instanceof NotFoundException &&
                        throwable.getMessage().contains("Wardrobe item not found with id: 999"))
                .verify();

        verify(itemRepository).findById(id);
        verify(userServiceClientWrapper, never()).getUserById(anyLong());
        verify(itemRepository, never()).save(any());
    }

    @Test
    void update_shouldThrowException_whenUserDoesNotExist() {
        // Given
        Long id = 1L;
        WardrobeItemDto updateDto = new WardrobeItemDto(
                ItemType.SHIRT, "Nike", "Blue", Season.SUMMER, "image.jpg", 999L
        );
        when(itemRepository.findById(id)).thenReturn(Mono.just(testItem));
        when(userServiceClientWrapper.getUserById(999L)).thenReturn(Mono.error(new IllegalArgumentException("User does not exist: 999")));

        // When
        Mono<WardrobeItemResponseDto> result = wardrobeItemService.update(id, updateDto);

        // Then
        StepVerifier.create(result)
                .expectErrorMatches(throwable ->
                        throwable instanceof IllegalArgumentException &&
                        throwable.getMessage().contains("User does not exist: 999"))
                .verify();

        verify(itemRepository).findById(id);
        verify(userServiceClientWrapper).getUserById(999L);
        verify(itemRepository, never()).save(any());
    }

    @Test
    void delete_shouldDeleteItem_whenExists() {
        // Given
        Long id = 1L;
        when(itemRepository.existsById(id)).thenReturn(Mono.just(true));
        when(itemRepository.deleteById(id)).thenReturn(Mono.empty());

        // When
        Mono<Void> result = wardrobeItemService.delete(id);

        // Then
        StepVerifier.create(result)
                .verifyComplete();

        verify(itemRepository).existsById(id);
        verify(itemRepository).deleteById(id);
    }

    @Test
    void delete_shouldThrowNotFoundException_whenNotExists() {
        // Given
        Long id = 999L;
        when(itemRepository.existsById(id)).thenReturn(Mono.just(false));

        // When
        Mono<Void> result = wardrobeItemService.delete(id);

        // Then
        StepVerifier.create(result)
                .expectErrorMatches(throwable ->
                        throwable instanceof NotFoundException &&
                        throwable.getMessage().contains("Wardrobe item not found with id: 999"))
                .verify();

        verify(itemRepository).existsById(id);
        verify(itemRepository, never()).deleteById(anyLong());
    }
}