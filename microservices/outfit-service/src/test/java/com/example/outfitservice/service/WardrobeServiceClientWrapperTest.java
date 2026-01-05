package com.example.outfitservice.service;

import com.example.outfitservice.client.WardrobeServiceClient;
import com.example.outfitservice.dto.WardrobeItemDto;
import com.example.outfitservice.entity.enums.ItemType;
import com.example.outfitservice.entity.enums.Season;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.function.Function;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WardrobeServiceClientWrapperTest {

    @Mock
    private WardrobeServiceClient wardrobeServiceClient;

    @Mock
    private CircuitBreakerFactory<?, ?> circuitBreakerFactory;

    @Mock
    private CircuitBreaker circuitBreaker;

    private WardrobeServiceClientWrapper wardrobeServiceClientWrapper;

    private WardrobeItemDto testItem;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        testItem = new WardrobeItemDto(
                1L,
                ItemType.SHIRT,
                "Nike",
                "Blue",
                Season.SUMMER,
                "test.jpg",
                1L
        );

        when(circuitBreakerFactory.create("wardrobe-service")).thenReturn((CircuitBreaker) circuitBreaker);
        wardrobeServiceClientWrapper = new WardrobeServiceClientWrapper(wardrobeServiceClient, circuitBreakerFactory);
    }

    @Test
    void getItemById_shouldReturnItem() {
        // Given
        Long itemId = 1L;
        when(wardrobeServiceClient.getItemById(itemId)).thenReturn(testItem);
        when(circuitBreaker.run(any(), any())).thenAnswer(inv -> {
            @SuppressWarnings("unchecked")
            Supplier<WardrobeItemDto> supplier = (Supplier<WardrobeItemDto>) inv.getArgument(0);
            return supplier.get();
        });

        // When
        WardrobeItemDto result = wardrobeServiceClientWrapper.getItemById(itemId);

        // Then
        assertThat(result).isEqualTo(testItem);
        verify(wardrobeServiceClient).getItemById(itemId);
        verify(circuitBreaker).run(any(), any());
    }

    @Test
    void getItemById_shouldThrowServiceUnavailable_whenCircuitBreakerOpens() {
        // Given
        Long itemId = 1L;
        when(circuitBreaker.run(any(), any())).thenAnswer(inv -> {
            @SuppressWarnings("unchecked")
            Function<Throwable, WardrobeItemDto> fallback = (Function<Throwable, WardrobeItemDto>) inv.getArgument(1);
            return fallback.apply(new RuntimeException("Service unavailable"));
        });

        // When & Then
        assertThatThrownBy(() -> wardrobeServiceClientWrapper.getItemById(itemId))
                .isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("status", HttpStatus.SERVICE_UNAVAILABLE)
                .hasMessageContaining("Wardrobe-service unavailable");

        verify(circuitBreaker).run(any(), any());
        verify(wardrobeServiceClient, never()).getItemById(eq(itemId));
    }
}
