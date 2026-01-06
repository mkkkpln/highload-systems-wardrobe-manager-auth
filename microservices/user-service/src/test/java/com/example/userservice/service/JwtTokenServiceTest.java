package com.example.userservice.service;

import com.example.userservice.entity.Role;
import com.example.userservice.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtTokenServiceTest {

    @Mock
    private JwtEncoder jwtEncoder;

    @Test
    void issueAccessToken_shouldBuildClaims_andReturnTokenValue() {
        JwtTokenService service = new JwtTokenService(jwtEncoder);
        ReflectionTestUtils.setField(service, "ttlSeconds", 1234L);

        User user = new User();
        user.setId(42L);
        user.setEmail("u@example.com");
        user.setRole(Role.ROLE_SUPERVISOR);

        when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenReturn(
                Jwt.withTokenValue("encoded-token")
                        .header("alg", "HS256")
                        .claim("dummy", "x")
                        .subject("u@example.com")
                        .issuedAt(Instant.now())
                        .expiresAt(Instant.now().plusSeconds(60))
                        .build()
        );

        String token = service.issueAccessToken(user);
        assertThat(token).isEqualTo("encoded-token");
        assertThat(service.getTtlSeconds()).isEqualTo(1234L);

        ArgumentCaptor<JwtEncoderParameters> captor = ArgumentCaptor.forClass(JwtEncoderParameters.class);
        verify(jwtEncoder).encode(captor.capture());

        JwtClaimsSet claims = captor.getValue().getClaims();
        assertThat(claims.getSubject()).isEqualTo("u@example.com");
        assertThat((String) claims.getClaim("userId")).isEqualTo("42");
        @SuppressWarnings("unchecked")
        List<String> roles = (List<String>) claims.getClaim("roles");
        assertThat(roles).containsExactly("ROLE_SUPERVISOR");
        assertThat(claims.getIssuedAt()).isNotNull();
        assertThat(claims.getExpiresAt()).isNotNull();
    }
}


