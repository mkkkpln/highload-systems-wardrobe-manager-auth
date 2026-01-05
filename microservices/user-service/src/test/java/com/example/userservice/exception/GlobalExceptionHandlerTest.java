package com.example.userservice.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleNotFoundException_shouldReturn404() {
        // Given
        NotFoundException exception = new NotFoundException("User not found with id: 999");

        // When
        ResponseEntity<String> response = handler.handleNotFoundException(exception);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isEqualTo("User not found with id: 999");
    }

    @Test
    void handleIllegalArgumentException_shouldReturn400() {
        // Given
        IllegalArgumentException exception = new IllegalArgumentException("User with email already exists: test@example.com");

        // When
        ResponseEntity<String> response = handler.handleIllegalArgumentException(exception);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isEqualTo("User with email already exists: test@example.com");
    }

    @Test
    void handleGenericException_shouldReturn500() {
        // Given
        Exception exception = new RuntimeException("Unexpected error");

        // When
        ResponseEntity<String> response = handler.handleGenericException(exception);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isEqualTo("Internal server error");
    }
}
