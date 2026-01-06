package com.example.userservice.dto.auth;

import com.example.userservice.entity.Role;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record RegisterRequestDto(
        @Email @NotBlank String email,
        @NotBlank @Size(min = 2, max = 100) String name,
        @NotBlank @Size(min = 6, max = 72) String password,
        @NotNull Role role
) {
}


