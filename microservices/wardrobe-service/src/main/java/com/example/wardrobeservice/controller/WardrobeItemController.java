package com.example.wardrobeservice.controller;

import com.example.wardrobeservice.dto.WardrobeItemDto;
import com.example.wardrobeservice.dto.WardrobeItemResponseDto;
import com.example.wardrobeservice.service.PagedResult;
import com.example.wardrobeservice.service.WardrobeItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/items")
@RequiredArgsConstructor
@Validated
public class WardrobeItemController {

    private final WardrobeItemService itemService;

    @Operation(summary = "Получить вещи с пагинацией", description = "Возвращает список вещей постранично и добавляет X-Total-Count в заголовок ответа")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Страница успешно получена"),
            @ApiResponse(responseCode = "400", description = "Некорректные параметры пагинации")
    })
    @GetMapping("/paged")
    public Mono<ResponseEntity<java.util.List<WardrobeItemResponseDto>>> getPagedWithCount(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int size) {
        
        return itemService.getItemsUpTo50(page, size)
                .map(pageResult -> {
                    HttpHeaders headers = new HttpHeaders();
                    headers.add("X-Total-Count", String.valueOf(pageResult.totalElements()));
                    return ResponseEntity.ok()
                            .headers(headers)
                            .body(pageResult.content());
                });
    }

    @Operation(summary = "Получить вещи (бесконечная прокрутка)", description = "Возвращает следующую часть списка без общего количества записей")
    @ApiResponse(responseCode = "200", description = "Часть списка успешно получена")
    @GetMapping("/scroll")
    public Flux<WardrobeItemResponseDto> getInfiniteScroll(
            @RequestParam(defaultValue = "0") @Min(0) int offset,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int limit) {
        return itemService.getInfiniteScroll(offset, limit);
    }

    @Operation(summary = "Получить вещь по ID", description = "Возвращает вещь по её уникальному идентификатору")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Вещь найдена"),
            @ApiResponse(responseCode = "404", description = "Вещь не найдена")
    })
    @GetMapping("/{id}")
    public Mono<ResponseEntity<WardrobeItemResponseDto>> getById(@PathVariable @Min(1) Long id) {
        return itemService.getById(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Добавить новую вещь", description = "Создает новую вещь в гардеробе пользователя")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Вещь успешно добавлена"),
            @ApiResponse(responseCode = "422", description = "Ошибка валидации данных")
    })
    @PostMapping
    public Mono<ResponseEntity<WardrobeItemResponseDto>> create(@Valid @RequestBody WardrobeItemDto dto) {
        return itemService.create(dto)
                .map(item -> ResponseEntity.status(HttpStatus.CREATED).body(item));
    }

    @Operation(summary = "Обновить вещь", description = "Обновляет данные существующей вещи по ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Вещь успешно обновлена"),
            @ApiResponse(responseCode = "404", description = "Вещь не найдена")
    })
    @PutMapping("/{id}")
    public Mono<ResponseEntity<WardrobeItemResponseDto>> update(@PathVariable @Min(1) Long id,
                                                        @Valid @RequestBody WardrobeItemDto dto) {
        return itemService.update(id, dto).map(ResponseEntity::ok);
    }


    @Operation(summary = "Удалить вещь", description = "Удаляет вещь по её ID")
    @ApiResponse(responseCode = "204", description = "Вещь успешно удалена")
    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> delete(@PathVariable @Min(1) Long id) {
        return itemService.delete(id)
                .thenReturn(ResponseEntity.noContent().<Void>build());
    }
}
