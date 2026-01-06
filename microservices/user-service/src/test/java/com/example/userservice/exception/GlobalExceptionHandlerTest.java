package com.example.userservice.exception;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.server.ResponseStatusException;

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

    @Test
    void handleResponseStatusException_shouldReturnStatusAndReason() {
        ResponseStatusException ex = new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        ResponseEntity<String> response = handler.handleResponseStatusException(ex);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isEqualTo("Invalid credentials");
    }

    @Test
    void handleAccessDeniedException_shouldReturn403() {
        ResponseEntity<String> response = handler.handleAccessDeniedException(new AccessDeniedException("x"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isEqualTo("Forbidden");
    }

    @Test
    void handleAuthenticationException_shouldReturn401() {
        ResponseEntity<String> response = handler.handleAuthenticationException(new BadCredentialsException("x"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isEqualTo("Unauthorized");
    }

    @Test
    void handleDataIntegrityViolation_shouldReturn409() {
        ResponseEntity<String> response = handler.handleDataIntegrityViolation(new DataIntegrityViolationException("x"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isEqualTo("Conflict");
    }
}
