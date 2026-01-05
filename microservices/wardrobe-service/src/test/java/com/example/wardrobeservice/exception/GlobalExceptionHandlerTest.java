package com.example.wardrobeservice.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleNotFoundException_shouldReturn404() {
        // Given
        NotFoundException exception = new NotFoundException("Wardrobe item not found with id: 999");

        // When
        var responseMono = handler.handleNotFoundException(exception);

        // Then
        StepVerifier.create(responseMono)
                .assertNext(response -> {
                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(response.getBody()).isEqualTo("Wardrobe item not found with id: 999");
                })
                .verifyComplete();
    }

    @Test
    void handleIllegalArgumentException_shouldReturn400() {
        // Given
        IllegalArgumentException exception = new IllegalArgumentException("User does not exist: 999");

        // When
        var responseMono = handler.handleIllegalArgumentException(exception);

        // Then
        StepVerifier.create(responseMono)
                .assertNext(response -> {
                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(response.getBody()).isEqualTo("User does not exist: 999");
                })
                .verifyComplete();
    }

    @Test
    void handleGenericException_shouldReturn500() {
        // Given
        Exception exception = new RuntimeException("Unexpected error");

        // When
        var responseMono = handler.handleGenericException(exception);

        // Then
        StepVerifier.create(responseMono)
                .assertNext(response -> {
                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
                    assertThat(response.getBody()).isEqualTo("Internal server error");
                })
                .verifyComplete();
    }
}
