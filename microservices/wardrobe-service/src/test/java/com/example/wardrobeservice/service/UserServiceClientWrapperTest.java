package com.example.wardrobeservice.service;

import com.example.wardrobeservice.client.UserServiceClient;
import com.example.wardrobeservice.dto.UserDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceClientWrapperTest {

    @Mock
    private UserServiceClient userServiceClient;

    @InjectMocks
    private UserServiceClientWrapper wrapper;

    @Test
    void getUserById_shouldReturnUser_whenClientSucceeds() {
        UserDto user = new UserDto(1L, "a@a", "A");
        when(userServiceClient.getUserById(1L)).thenReturn(Mono.just(user));

        StepVerifier.create(wrapper.getUserById(1L))
                .assertNext(u -> assertThat(u.id()).isEqualTo(1L))
                .verifyComplete();
    }

    @Test
    void fallback_shouldReturn503ResponseStatusException() throws Exception {
        Method m = UserServiceClientWrapper.class.getDeclaredMethod("getUserByIdFallback", Long.class, Throwable.class);
        m.setAccessible(true);

        @SuppressWarnings("unchecked")
        Mono<UserDto> fallback = (Mono<UserDto>) m.invoke(wrapper, 1L, new RuntimeException("boom"));

        StepVerifier.create(fallback)
                .expectErrorMatches(t ->
                        t instanceof ResponseStatusException rse
                                && rse.getStatusCode().value() == HttpStatus.SERVICE_UNAVAILABLE.value())
                .verify();
    }

    @Test
    void getUserById_shouldPropagateError_whenClientErrors_directly() {
        when(userServiceClient.getUserById(anyLong())).thenReturn(Mono.error(new RuntimeException("boom")));

        StepVerifier.create(wrapper.getUserById(1L))
                .expectError(RuntimeException.class)
                .verify();
    }
}
