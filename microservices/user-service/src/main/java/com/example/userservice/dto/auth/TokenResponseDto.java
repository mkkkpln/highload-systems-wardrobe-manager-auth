package com.example.userservice.dto.auth;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record TokenResponseDto(
        String accessToken,
        String tokenType,
        long expiresIn
) {
    public static TokenResponseDto bearer(String accessToken, long expiresInSeconds) {
        return new TokenResponseDto(accessToken, "Bearer", expiresInSeconds);
    }
}


