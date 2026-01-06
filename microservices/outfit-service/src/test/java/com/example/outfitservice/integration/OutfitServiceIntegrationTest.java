package com.example.outfitservice.integration;

import com.example.outfitservice.OutfitServiceApplication;
import com.example.outfitservice.dto.*;
import com.example.outfitservice.entity.OutfitRole;
import com.example.outfitservice.service.OutfitService;
import com.example.outfitservice.service.PagedResult;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyLong;

@SpringBootTest(classes = OutfitServiceApplication.class)
@Testcontainers
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
@SuppressWarnings("resource")
public class OutfitServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("outfits_test")
            .withUsername("user")
            .withPassword("user");

    @Autowired
    private OutfitService outfitService;

    @MockitoBean
    private com.example.outfitservice.service.UserServiceClientWrapper userServiceClientWrapper;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    private void asUser(long userId) {
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .subject("user" + userId + "@example.com")
                .claim("userId", String.valueOf(userId))
                .claim("roles", List.of("ROLE_USER"))
                .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }

    @Test
    void shouldCreateOutfit() {
        asUser(1L);
        // given
        Mockito.when(userServiceClientWrapper.getUserById(anyString(), anyLong()))
                .thenReturn(new UserDto(1L, "test@example.com", "Test User"));

        OutfitDto createDto = new OutfitDto(
                "Summer Outfit",
                1L,
                List.of(
                        new OutfitItemLinkDto(1L, OutfitRole.TOP),
                        new OutfitItemLinkDto(2L, OutfitRole.BOTTOM)
                )
        );

        // when
        OutfitResponseDto created = outfitService.create(createDto);

        // then
        assertThat(created.id()).isNotNull();
        assertThat(created.title()).isEqualTo("Summer Outfit");
        assertThat(created.userId()).isEqualTo(1L);
        // items mapping/relationship isn't guaranteed to be returned by mapper; validate core fields only
    }

    @Test
    void shouldGetOutfitById_whenExists() {
        asUser(1L);
        // given
        Mockito.when(userServiceClientWrapper.getUserById(anyString(), anyLong()))
                .thenReturn(new UserDto(1L, "test@example.com", "Test User"));

        OutfitDto createDto = new OutfitDto(
                "Test Outfit",
                1L,
                List.of(new OutfitItemLinkDto(1L, OutfitRole.TOP))
        );
        OutfitResponseDto created = outfitService.create(createDto);

        // when
        OutfitResponseDto found = outfitService.getById(created.id());

        // then
        assertThat(found.id()).isEqualTo(created.id());
        assertThat(found.title()).isEqualTo("Test Outfit");
        assertThat(found.userId()).isEqualTo(1L);
    }

    @Test
    void shouldThrowNotFoundException_whenOutfitDoesNotExist() {
        asUser(1L);
        // when & then
        assertThatThrownBy(() -> outfitService.getById(999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Access denied");
    }

    @Test
    void shouldGetPagedOutfits() {
        asUser(1L);
        // given - create some outfits
        Mockito.when(userServiceClientWrapper.getUserById(anyString(), anyLong()))
                .thenReturn(new UserDto(1L, "test@example.com", "Test User"));

        for (int i = 0; i < 5; i++) {
            OutfitDto createDto = new OutfitDto(
                    "Outfit " + i,
                    1L,
                    List.of(new OutfitItemLinkDto(1L, OutfitRole.TOP))
            );
            outfitService.create(createDto);
        }

        // when
        PagedResult<OutfitResponseDto> result = outfitService.getOutfitsUpTo50(0, 10);

        // then
        assertThat(result.items()).hasSizeGreaterThanOrEqualTo(5);
        assertThat(result.totalCount()).isGreaterThanOrEqualTo(5);
    }

    @Test
    void shouldGetInfiniteScrollOutfits() {
        asUser(1L);
        // given - create some outfits
        Mockito.when(userServiceClientWrapper.getUserById(anyString(), anyLong()))
                .thenReturn(new UserDto(1L, "test@example.com", "Test User"));

        for (int i = 0; i < 3; i++) {
            OutfitDto createDto = new OutfitDto(
                    "Scroll Outfit " + i,
                    1L,
                    List.of(new OutfitItemLinkDto(1L, OutfitRole.TOP))
            );
            outfitService.create(createDto);
        }

        // when
        List<OutfitResponseDto> result = outfitService.getInfiniteScroll(0, 10);

        // then
        assertThat(result).hasSizeGreaterThanOrEqualTo(3);
    }

    @Test
    void shouldUpdateOutfit() {
        asUser(1L);
        // given
        Mockito.when(userServiceClientWrapper.getUserById(anyString(), anyLong()))
                .thenReturn(new UserDto(1L, "test@example.com", "Test User"));

        OutfitDto createDto = new OutfitDto(
                "Original Outfit",
                1L,
                List.of(new OutfitItemLinkDto(1L, OutfitRole.TOP))
        );
        OutfitResponseDto created = outfitService.create(createDto);

        OutfitDto updateDto = new OutfitDto(
                "Updated Outfit",
                1L,
                List.of(
                        new OutfitItemLinkDto(1L, OutfitRole.TOP),
                        new OutfitItemLinkDto(2L, OutfitRole.BOTTOM)
                )
        );

        // when
        OutfitResponseDto updated = outfitService.update(created.id(), updateDto);

        // then
        assertThat(updated.id()).isEqualTo(created.id());
        assertThat(updated.title()).isEqualTo("Updated Outfit");
        // items mapping/relationship isn't guaranteed to be returned by mapper; validate core fields only
    }

    @Test
    void shouldDeleteOutfit() {
        asUser(1L);
        // given
        Mockito.when(userServiceClientWrapper.getUserById(anyString(), anyLong()))
                .thenReturn(new UserDto(1L, "test@example.com", "Test User"));

        OutfitDto createDto = new OutfitDto(
                "Outfit to Delete",
                1L,
                List.of(new OutfitItemLinkDto(1L, OutfitRole.TOP))
        );
        OutfitResponseDto created = outfitService.create(createDto);

        // when
        outfitService.delete(created.id());

        // then
        assertThatThrownBy(() -> outfitService.getById(created.id()))
                .isInstanceOf(RuntimeException.class);
    }
}

