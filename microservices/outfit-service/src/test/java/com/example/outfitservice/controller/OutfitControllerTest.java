package com.example.outfitservice.controller;

import com.example.outfitservice.dto.OutfitItemLinkDto;
import com.example.outfitservice.dto.OutfitResponseDto;
import com.example.outfitservice.entity.OutfitRole;
import com.example.outfitservice.exception.GlobalExceptionHandler;
import com.example.outfitservice.exception.NotFoundException;
import com.example.outfitservice.service.OutfitService;
import com.example.outfitservice.service.PagedResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = OutfitController.class)
@Import(GlobalExceptionHandler.class)
class OutfitControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OutfitService outfitService;

    @Test
    void getById_shouldReturn200_whenFound() throws Exception {
        OutfitResponseDto dto = new OutfitResponseDto(
                1L,
                "Test Outfit",
                10L,
                List.of(new OutfitItemLinkDto(1L, OutfitRole.TOP))
        );

        when(outfitService.getById(1L)).thenReturn(dto);

        mockMvc.perform(get("/outfits/1").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Test Outfit"))
                .andExpect(jsonPath("$.user_id").value(10))
                .andExpect(jsonPath("$.items[0].item_id").value(1))
                .andExpect(jsonPath("$.items[0].role").value("TOP"));
    }

    @Test
    void getById_shouldReturn404_whenNotFound() throws Exception {
        when(outfitService.getById(999L)).thenThrow(new NotFoundException("Outfit not found with id: 999"));

        mockMvc.perform(get("/outfits/999").with(jwt()))
                .andExpect(status().isNotFound());
    }

    @Test
    void getPaged_shouldReturnHeaderAndBody() throws Exception {
        OutfitResponseDto dto = new OutfitResponseDto(1L, "Outfit", 10L, List.of());
        when(outfitService.getOutfitsUpTo50(0, 10)).thenReturn(new PagedResult<>(List.of(dto), 123));

        mockMvc.perform(get("/outfits/paged?page=0&size=10").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Total-Count", "123"))
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void getInfiniteScroll_shouldReturnList() throws Exception {
        OutfitResponseDto dto = new OutfitResponseDto(1L, "Outfit", 10L, List.of());
        when(outfitService.getInfiniteScroll(0, 10)).thenReturn(List.of(dto));

        mockMvc.perform(get("/outfits/scroll?offset=0&limit=10").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void create_shouldReturn201() throws Exception {
        OutfitResponseDto created = new OutfitResponseDto(1L, "Created", 10L, List.of());
        when(outfitService.create(any())).thenReturn(created);

        mockMvc.perform(post("/outfits")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Created",
                                  "user_id": 10,
                                  "items": []
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Created"));
    }

    @Test
    void create_shouldReturn400_whenInvalidBody() throws Exception {
        mockMvc.perform(post("/outfits")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "user_id": 10,
                                  "items": []
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void update_shouldReturn200() throws Exception {
        OutfitResponseDto updated = new OutfitResponseDto(1L, "Updated", 10L, List.of());
        when(outfitService.update(eq(1L), any())).thenReturn(updated);

        mockMvc.perform(put("/outfits/1")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "id": 1,
                                  "title": "Updated",
                                  "user_id": 10,
                                  "items": []
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated"));
    }

    @Test
    void delete_shouldReturn204() throws Exception {
        doNothing().when(outfitService).delete(1L);

        mockMvc.perform(delete("/outfits/1").with(jwt()))
                .andExpect(status().isNoContent());
    }
}

