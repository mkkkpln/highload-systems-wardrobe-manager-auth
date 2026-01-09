package com.example.userservice.dto.auth;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record LoginRequestDto(
        @NotBlank(message = "Email must not be blank")
        @Size(min = 5, max = 320, message = "User email must be between 5 and 320 characters")
        @Email(message = "Email must be valid")
        String email,

        @NotBlank(message = "Password must not be blank")
        @Size(min = 5, max = 255, message = "Password must be between 5 and 255 characters")
        String password
) {
}


