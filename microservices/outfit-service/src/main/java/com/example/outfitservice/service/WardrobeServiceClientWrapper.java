package com.example.outfitservice.service;

import com.example.outfitservice.client.WardrobeServiceClient;
import com.example.outfitservice.dto.WardrobeItemDto;
import feign.FeignException;
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

    public WardrobeItemDto getItemById(String authorization, Long id) {
        return circuitBreakerFactory
                .create("wardrobe-service")
                .run(
                        () -> client.getItemById(authorization, id),
                        throwable -> {
                            if (throwable instanceof FeignException.NotFound) {
                                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Wardrobe item not found with id: " + id, throwable);
                            }
                            if (throwable instanceof FeignException.Forbidden) {
                                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied", throwable);
                            }
                            if (throwable instanceof FeignException.Unauthorized) {
                                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized", throwable);
                            }
                            if (throwable instanceof FeignException fe) {
                                HttpStatus status = HttpStatus.resolve(fe.status());
                                if (status != null && status.is4xxClientError()) {
                                    throw new ResponseStatusException(status, "Wardrobe-service returned " + fe.status(), fe);
                                }
                            }
                            throw new ResponseStatusException(
                                    HttpStatus.SERVICE_UNAVAILABLE,
                                    "Wardrobe-service unavailable (circuit breaker)"
                            );
                        }
                );
    }
}
