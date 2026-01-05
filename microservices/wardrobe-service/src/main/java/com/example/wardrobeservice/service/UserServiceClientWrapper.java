package com.example.wardrobeservice.service;

import com.example.wardrobeservice.client.UserServiceClient;
import com.example.wardrobeservice.dto.UserDto;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class UserServiceClientWrapper {

    private final UserServiceClient userServiceClient;

    @CircuitBreaker(name = "user-service", fallbackMethod = "getUserByIdFallback")
    public Mono<UserDto> getUserById(Long id) {
        return userServiceClient.getUserById(id);
    }

    // Fallback метод с теми же аргументами + Throwable/Exception в конце
    private Mono<UserDto> getUserByIdFallback(Long id, Throwable ex) {
        return Mono.error(new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "User service is currently unavailable. Please try again later."
        ));
    }
}

