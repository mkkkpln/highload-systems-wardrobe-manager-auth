package com.example.outfitservice.controller;

import com.example.outfitservice.dto.OutfitDto;
import com.example.outfitservice.dto.OutfitResponseDto;
import com.example.outfitservice.service.OutfitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/outfits")
@RequiredArgsConstructor
@Validated
public class OutfitController {

    private final OutfitService outfitService;

    @Operation(summary = "Получить образ по ID", description = "Возвращает образ с указанным идентификатором")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Образ найден"),
            @ApiResponse(responseCode = "404", description = "Образ не найден")
    })
    @GetMapping("/{id}")
    public ResponseEntity<OutfitResponseDto> getById(@PathVariable @Min(1) Long id) {
        return ResponseEntity.ok(outfitService.getById(id));
    }

    @Operation(summary = "Создать новый образ", description = "Создает новый образ на основе переданных данных")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Образ успешно создан"),
            @ApiResponse(responseCode = "422", description = "Ошибка валидации данных")
    })
    @PostMapping
    public ResponseEntity<OutfitResponseDto> create(@Valid @RequestBody OutfitDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(outfitService.create(dto));
    }

    @Operation(summary = "Обновить существующий образ", description = "Обновляет данные образа по указанному ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Образ успешно обновлен"),
            @ApiResponse(responseCode = "404", description = "Образ не найден")
    })
    @PutMapping("/{id}")
    public ResponseEntity<OutfitResponseDto> update(@PathVariable @Min(1) Long id,
                                            @Valid @RequestBody OutfitDto dto) {
        return ResponseEntity.ok(outfitService.update(id, dto));
    }

    @Operation(summary = "Получить образы с пагинацией",
            description = "Возвращает список образов постранично (не более 50 за запрос). В заголовке ответа указывается общее количество записей.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Образы успешно получены")
    })
    @GetMapping("/paged")
    public ResponseEntity<List<OutfitResponseDto>> getPaged(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int size) {
        var result = outfitService.getOutfitsUpTo50(page, size);
        return ResponseEntity
                .ok()
                .header("X-Total-Count", String.valueOf(result.totalCount()))
                .body(result.items());
    }

    @Operation(summary = "Бесконечная прокрутка образов",
            description = "Возвращает часть списка образов без указания общего количества записей (для 'ленты').")
    @GetMapping("/scroll")
    public ResponseEntity<List<OutfitResponseDto>> getInfiniteScroll(
            @RequestParam(defaultValue = "0") @Min(0) int offset,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int limit) {
        return ResponseEntity.ok(outfitService.getInfiniteScroll(offset, limit));
    }

    @Operation(summary = "Удалить образ", description = "Удаляет образ по указанному ID")
    @ApiResponse(responseCode = "204", description = "Образ успешно удален")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable @Min(1) Long id) {
        outfitService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
