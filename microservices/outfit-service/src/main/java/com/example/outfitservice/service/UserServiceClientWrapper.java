package com.example.outfitservice.service;

import com.example.outfitservice.client.UserServiceClient;
import com.example.outfitservice.dto.UserDto;
import com.example.outfitservice.exception.DownstreamServiceUnavailableException;
import feign.FeignException;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceClientWrapper {

    private final UserServiceClient client;
    private final CircuitBreakerRegistry circuitBreakerRegistry;

    @CircuitBreaker(
            name = "user-service",
            fallbackMethod = "getUserByIdFallback"
    )
    public UserDto getUserById(Long id) {
        return client.getUserById(id);
    }

    public UserDto getUserByIdFallback(Long id, Throwable ex) {
        // 404 от user-service - это бизнес-ошибка (пользователь не найден), не 503.
        if (ex instanceof FeignException.NotFound) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "User not found with id: " + id,
                    ex
            );
        }

        var cb = circuitBreakerRegistry.circuitBreaker("user-service");
        var state = cb.getState().name();
        log.warn("User-service call failed. circuitBreaker=user-service state={} userId={} ex={}",
                state, id, ex.getClass().getSimpleName(), ex);

        throw new DownstreamServiceUnavailableException(
                "user-service",
                "user-service",
                state,
                "User-service unavailable (circuit breaker state=" + state + ")",
                ex
        );
    }
}
