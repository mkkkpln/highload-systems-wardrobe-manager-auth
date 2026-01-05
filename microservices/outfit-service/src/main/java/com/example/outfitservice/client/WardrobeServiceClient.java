package com.example.outfitservice.client;

import com.example.outfitservice.dto.WardrobeItemDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "wardrobe-service")
public interface WardrobeServiceClient {

    @GetMapping("/items/{id}")
    WardrobeItemDto getItemById(@PathVariable Long id);
}
