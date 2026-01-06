package com.example.userservice.service;

import com.example.userservice.dto.auth.LoginRequestDto;
import com.example.userservice.dto.auth.RegisterRequestDto;
import com.example.userservice.dto.auth.TokenResponseDto;
import com.example.userservice.entity.Role;
import com.example.userservice.entity.User;
import com.example.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;

    public TokenResponseDto login(LoginRequestDto req) {
        User user = userRepository.findByEmail(req.email())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        String token = jwtTokenService.issueAccessToken(user);
        return TokenResponseDto.bearer(token, jwtTokenService.getTtlSeconds());
    }

    @Transactional
    public User register(RegisterRequestDto req) {
        if (userRepository.existsByEmail(req.email())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User with email already exists: " + req.email());
        }

        User user = new User();
        user.setEmail(req.email());
        user.setName(req.name());
        user.setPasswordHash(passwordEncoder.encode(req.password()));
        user.setRole(req.role() == null ? Role.ROLE_USER : req.role());
        return userRepository.save(user);
    }
}


