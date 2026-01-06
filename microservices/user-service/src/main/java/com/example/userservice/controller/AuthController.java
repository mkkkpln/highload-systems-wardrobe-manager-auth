package com.example.userservice.controller;

import com.example.userservice.dto.UserResponseDto;
import com.example.userservice.dto.auth.LoginRequestDto;
import com.example.userservice.dto.auth.MeResponseDto;
import com.example.userservice.dto.auth.RegisterRequestDto;
import com.example.userservice.dto.auth.TokenResponseDto;
import com.example.userservice.mapper.UserMapper;
import com.example.userservice.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
import java.util.List;

@RestController
@RequestMapping("/users/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserMapper userMapper;

    @Operation(summary = "Логин (JWT)", security = {})
    @PostMapping("/login")
    public ResponseEntity<TokenResponseDto> login(@Valid @RequestBody LoginRequestDto req) {
        return ResponseEntity.ok(authService.login(req));
    }

    @Operation(summary = "[SUPERVISOR] Создать пользователя (только супервайзер)")
    @PostMapping("/register")
    @PreAuthorize("hasRole('SUPERVISOR')")
    public ResponseEntity<UserResponseDto> register(@Valid @RequestBody RegisterRequestDto req) {
        var created = authService.register(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(userMapper.toDto(created));
    }

    @Operation(summary = "Текущий пользователь (whoami)")
    @GetMapping("/me")
    public ResponseEntity<MeResponseDto> me(Authentication authentication) {
        if (!(authentication instanceof JwtAuthenticationToken jwtAuth)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Jwt jwt = jwtAuth.getToken();
        String email = jwt.getSubject();
        String userId = jwt.getClaimAsString("userId");
        Object rolesClaim = jwt.getClaims().get("roles");

        List<String> roles;
        if (rolesClaim instanceof Collection<?> c) {
            roles = c.stream().map(String::valueOf).toList();
        } else if (rolesClaim == null) {
            roles = List.of();
        } else {
            roles = List.of(String.valueOf(rolesClaim));
        }

        return ResponseEntity.ok(new MeResponseDto(userId, email, roles));
    }
}


