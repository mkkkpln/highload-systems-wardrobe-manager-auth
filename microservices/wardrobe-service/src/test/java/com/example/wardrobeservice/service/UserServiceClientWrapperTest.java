package com.example.wardrobeservice.service;

import com.example.wardrobeservice.client.UserServiceClient;
import com.example.wardrobeservice.dto.UserDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceClientWrapperTest {

    @Mock
    private UserServiceClient userServiceClient;

    @InjectMocks
    private UserServiceClientWrapper userServiceClientWrapper;

    @Test
    void getUserById_shouldReturnUser_whenServiceAvailable() {
        // given
        Long userId = 1L;
        UserDto expectedUser = new UserDto(userId, "test@example.com", "Test User");

        when(userServiceClient.getUserById(userId)).thenReturn(Mono.just(expectedUser));

        // when
        Mono<UserDto> result = userServiceClientWrapper.getUserById(userId);

        // then
        StepVerifier.create(result)
                .expectNext(expectedUser)
                .verifyComplete();
    }

    @Test
    void getUserById_shouldHandleServiceError() {
        // given
        Long userId = 1L;

        when(userServiceClient.getUserById(anyLong()))
                .thenReturn(Mono.error(new RuntimeException("Service unavailable")));

        // when
        Mono<UserDto> result = userServiceClientWrapper.getUserById(userId);

        // then
        StepVerifier.create(result)
                .expectError(RuntimeException.class)
                .verify();
    }
}
