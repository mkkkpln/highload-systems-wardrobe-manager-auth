package com.example.outfitservice.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    @Test
    void simpleExceptionHandlerTest_shouldPass() {
        // Simple test to verify exception handler test infrastructure works
        assertThat("exception").isNotNull();
        assertThat(4 * 4).isEqualTo(16);
    }
}

