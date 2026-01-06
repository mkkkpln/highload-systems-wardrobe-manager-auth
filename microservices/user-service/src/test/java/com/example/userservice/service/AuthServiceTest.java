package com.example.userservice.service;

import com.example.userservice.dto.auth.LoginRequestDto;
import com.example.userservice.dto.auth.RegisterRequestDto;
import com.example.userservice.dto.auth.TokenResponseDto;
import com.example.userservice.entity.Role;
import com.example.userservice.entity.User;
import com.example.userservice.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenService jwtTokenService;

    @InjectMocks
    private AuthService authService;

    @Test
    void login_shouldReturnBearerToken_whenCredentialsValid() {
        User user = new User();
        user.setId(10L);
        user.setEmail("user@example.com");
        user.setPasswordHash("$2a$hash");
        user.setRole(Role.ROLE_USER);

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password", "$2a$hash")).thenReturn(true);
        when(jwtTokenService.issueAccessToken(user)).thenReturn("jwt-token");
        when(jwtTokenService.getTtlSeconds()).thenReturn(3600L);

        TokenResponseDto resp = authService.login(new LoginRequestDto("user@example.com", "password"));

        assertThat(resp.accessToken()).isEqualTo("jwt-token");
        assertThat(resp.tokenType()).isEqualTo("Bearer");
        assertThat(resp.expiresIn()).isEqualTo(3600L);
        verify(userRepository).findByEmail("user@example.com");
        verify(passwordEncoder).matches("password", "$2a$hash");
    }

    @Test
    void login_shouldReturn401_whenEmailNotFound() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequestDto("missing@example.com", "password")))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void login_shouldReturn401_whenPasswordInvalid() {
        User user = new User();
        user.setEmail("user@example.com");
        user.setPasswordHash("$2a$hash");

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("bad", "$2a$hash")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequestDto("user@example.com", "bad")))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void register_shouldCreateUser_withHashedPassword_andDefaultRole() {
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password")).thenReturn("$2a$hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User created = authService.register(new RegisterRequestDto("new@example.com", "New", "password", null));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();

        assertThat(saved.getEmail()).isEqualTo("new@example.com");
        assertThat(saved.getName()).isEqualTo("New");
        assertThat(saved.getPasswordHash()).isEqualTo("$2a$hashed");
        assertThat(saved.getRole()).isEqualTo(Role.ROLE_USER);

        // method result is whatever repository returned (same instance here)
        assertThat(created.getEmail()).isEqualTo("new@example.com");
        assertThat(created.getRole()).isEqualTo(Role.ROLE_USER);
    }

    @Test
    void register_shouldAllowExplicitRole() {
        when(userRepository.existsByEmail("sup@example.com")).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("$2a$hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User created = authService.register(new RegisterRequestDto("sup@example.com", "Sup", "password", Role.ROLE_SUPERVISOR));
        assertThat(created.getRole()).isEqualTo(Role.ROLE_SUPERVISOR);
    }

    @Test
    void register_shouldReturn400_whenEmailAlreadyExists() {
        when(userRepository.existsByEmail("exists@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(new RegisterRequestDto("exists@example.com", "Name", "password", null)))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }
}


