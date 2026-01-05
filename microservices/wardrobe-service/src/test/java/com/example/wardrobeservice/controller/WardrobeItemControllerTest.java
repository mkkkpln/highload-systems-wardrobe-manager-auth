package com.example.wardrobeservice.controller;

import com.example.wardrobeservice.dto.WardrobeItemDto;
import com.example.wardrobeservice.dto.WardrobeItemResponseDto;
import com.example.wardrobeservice.entity.enums.ItemType;
import com.example.wardrobeservice.entity.enums.Season;
import com.example.wardrobeservice.exception.GlobalExceptionHandler;
import com.example.wardrobeservice.service.PagedResult;
import com.example.wardrobeservice.service.WardrobeItemService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.data.r2dbc.R2dbcDataAutoConfiguration;
import org.springframework.boot.autoconfigure.data.r2dbc.R2dbcRepositoriesAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@SpringBootTest
@AutoConfigureWebTestClient
class WardrobeItemControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private WardrobeItemService itemService;

    @Test
    void getById_shouldReturn200_whenFound() {
        WardrobeItemResponseDto dto = new WardrobeItemResponseDto(
                1L,
                ItemType.SHIRT,
                "Nike",
                "Blue",
                Season.SUMMER,
                "img.jpg",
                10L
        );

        when(itemService.getById(1L)).thenReturn(Mono.just(dto));

        webTestClient.get().uri("/items/1")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(1)
                .jsonPath("$.type").isEqualTo("SHIRT")
                .jsonPath("$.owner_id").isEqualTo(10);
    }

    @Test
    void getById_shouldReturn404_whenNotFound() {
        when(itemService.getById(999L)).thenReturn(Mono.empty());

        webTestClient.get().uri("/items/999")
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void getPagedWithCount_shouldReturnHeaderAndBody() {
        WardrobeItemResponseDto dto = new WardrobeItemResponseDto(
                1L, ItemType.SHIRT, "Nike", "Blue", Season.SUMMER, "img.jpg", 10L
        );
        when(itemService.getItemsUpTo50(0, 10)).thenReturn(Mono.just(new PagedResult<>(List.of(dto), 123)));

        webTestClient.get().uri("/items/paged?page=0&size=10")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals("X-Total-Count", "123")
                .expectBody()
                .jsonPath("$[0].id").isEqualTo(1);
    }

    @Test
    void getInfiniteScroll_shouldReturnFlux() {
        WardrobeItemResponseDto dto = new WardrobeItemResponseDto(
                1L, ItemType.SHIRT, "Nike", "Blue", Season.SUMMER, "img.jpg", 10L
        );
        when(itemService.getInfiniteScroll(0, 10)).thenReturn(Flux.just(dto));

        webTestClient.get().uri("/items/scroll?offset=0&limit=10")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[0].id").isEqualTo(1);
    }

    @Test
    void create_shouldReturn201() {
        WardrobeItemResponseDto created = new WardrobeItemResponseDto(
                1L, ItemType.SHIRT, "Nike", "Blue", Season.SUMMER, "img.jpg", 10L
        );
        when(itemService.create(any())).thenReturn(Mono.just(created));

        webTestClient.post().uri("/items")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "type": "SHIRT",
                          "brand": "Nike",
                          "color": "Blue",
                          "season": "SUMMER",
                          "image_url": "img.jpg",
                          "owner_id": 10
                        }
                        """)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.id").isEqualTo(1);
    }

    @Test
    void create_shouldReturn400_whenInvalidBody() {
        when(itemService.create(any())).thenReturn(Mono.empty());

        webTestClient.post().uri("/items")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "type": "SHIRT",
                          "season": "SUMMER",
                          "image_url": "img.jpg"
                        }
                        """)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void update_shouldReturn200() {
        WardrobeItemResponseDto updated = new WardrobeItemResponseDto(
                1L, ItemType.SHIRT, "Nike", "Blue", Season.SUMMER, "img.jpg", 10L
        );
        when(itemService.update(eq(1L), any())).thenReturn(Mono.just(updated));

        webTestClient.put().uri("/items/1")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {
                          "id": 1,
                          "type": "SHIRT",
                          "brand": "Nike",
                          "color": "Blue",
                          "season": "SUMMER",
                          "image_url": "img.jpg",
                          "owner_id": 10
                        }
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(1);
    }

    @Test
    void delete_shouldReturn204() {
        when(itemService.delete(1L)).thenReturn(Mono.empty());

        webTestClient.delete().uri("/items/1")
                .exchange()
                .expectStatus().isNoContent();
    }
}
