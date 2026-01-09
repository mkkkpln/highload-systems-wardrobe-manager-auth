package com.example.outfitservice.client;

import com.example.outfitservice.dto.WardrobeItemDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(
        name = "wardrobe-service",
        url = "${clients.wardrobe-service.base-url:http://wardrobe-service:8082}"
)
public interface WardrobeServiceClient {

    @GetMapping("/items/{id}")
    WardrobeItemDto getItemById(@RequestHeader("Authorization") String authorization,
                                @PathVariable Long id);
}
