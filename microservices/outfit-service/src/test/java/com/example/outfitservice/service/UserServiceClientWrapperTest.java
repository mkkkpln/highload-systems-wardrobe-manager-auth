package com.example.outfitservice.service;

import com.example.outfitservice.client.UserServiceClient;
import com.example.outfitservice.dto.UserDto;
import com.example.outfitservice.exception.DownstreamServiceUnavailableException;
import feign.Request;
import feign.RequestTemplate;
import feign.FeignException;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

class UserServiceClientWrapperTest {

    private static FeignException feignException(int status) {
        Request req = Request.create(
                Request.HttpMethod.GET,
                "/users/1",
                Collections.emptyMap(),
                null,
                new RequestTemplate()
        );
        return FeignException.errorStatus("GET /users/1", feign.Response.builder()
                .status(status)
                .reason("x")
                .request(req)
                .headers(Collections.emptyMap())
                .body("x", StandardCharsets.UTF_8)
                .build());
    }

    @Test
    void fallback_shouldMapNotFoundTo404() {
        UserServiceClient client = Mockito.mock(UserServiceClient.class);
        CircuitBreakerRegistry registry = CircuitBreakerRegistry.ofDefaults();
        UserServiceClientWrapper wrapper = new UserServiceClientWrapper(client, registry);

        assertThatThrownBy(() -> wrapper.getUserByIdFallback("Bearer t", 1L, feignException(404)))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(NOT_FOUND);
    }

    @Test
    void fallback_shouldMapUnauthorizedTo401() {
        UserServiceClient client = Mockito.mock(UserServiceClient.class);
        CircuitBreakerRegistry registry = CircuitBreakerRegistry.ofDefaults();
        UserServiceClientWrapper wrapper = new UserServiceClientWrapper(client, registry);

        assertThatThrownBy(() -> wrapper.getUserByIdFallback("Bearer t", 1L, feignException(401)))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(UNAUTHORIZED);
    }

    @Test
    void fallback_shouldMapForbiddenTo403() {
        UserServiceClient client = Mockito.mock(UserServiceClient.class);
        CircuitBreakerRegistry registry = CircuitBreakerRegistry.ofDefaults();
        UserServiceClientWrapper wrapper = new UserServiceClientWrapper(client, registry);

        assertThatThrownBy(() -> wrapper.getUserByIdFallback("Bearer t", 1L, feignException(403)))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(FORBIDDEN);
    }

    @Test
    void fallback_shouldThrowDownstreamUnavailable_forOtherErrors_andIncludeCircuitBreakerState() {
        UserServiceClient client = Mockito.mock(UserServiceClient.class);
        CircuitBreakerRegistry registry = CircuitBreakerRegistry.ofDefaults();
        UserServiceClientWrapper wrapper = new UserServiceClientWrapper(client, registry);

        assertThatThrownBy(() -> wrapper.getUserByIdFallback("Bearer t", 1L, new RuntimeException("boom")))
                .isInstanceOf(DownstreamServiceUnavailableException.class)
                .satisfies(ex -> {
                    DownstreamServiceUnavailableException d = (DownstreamServiceUnavailableException) ex;
                    assertThat(d.getCircuitBreakerState()).isNotBlank();
                });
    }

    @Test
    void getUserById_shouldPassAuthorizationHeader_toFeignClient() {
        UserServiceClient client = Mockito.mock(UserServiceClient.class);
        CircuitBreakerRegistry registry = CircuitBreakerRegistry.ofDefaults();
        UserServiceClientWrapper wrapper = new UserServiceClientWrapper(client, registry);

        when(client.getUserById("Bearer t", 1L)).thenReturn(new UserDto(1L, "a@a", "A"));

        UserDto dto = wrapper.getUserById("Bearer t", 1L);
        assertThat(dto.id()).isEqualTo(1L);
    }
}


