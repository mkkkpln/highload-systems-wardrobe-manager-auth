package com.example.outfitservice.service;

import com.example.outfitservice.client.WardrobeServiceClient;
import com.example.outfitservice.dto.WardrobeItemDto;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class WardrobeServiceClientWrapper {

    private final WardrobeServiceClient client;
    private final CircuitBreakerFactory<?, ?> circuitBreakerFactory;

    public WardrobeItemDto getItemById(Long id) {
        return circuitBreakerFactory
                .create("wardrobe-service")
                .run(
                        () -> client.getItemById(id),
                        throwable -> {
                            throw new ResponseStatusException(
                                    HttpStatus.SERVICE_UNAVAILABLE,
                                    "Wardrobe-service unavailable (circuit breaker)"
                            );
                        }
                );
    }
}
