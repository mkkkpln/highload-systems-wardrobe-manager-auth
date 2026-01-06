package com.example.userservice.config;

import org.junit.jupiter.api.Test;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityConfigTest {

    private static Jwt jwtWithClaims(Map<String, Object> claims) {
        return new Jwt(
                "t",
                Instant.now(),
                Instant.now().plusSeconds(60),
                Map.of("alg", "HS256"),
                claims
        );
    }

    @Test
    void jwtAuthenticationConverter_shouldExtractAuthoritiesFromCollectionRoles() {
        SecurityConfig cfg = new SecurityConfig();
        Converter<Jwt, ? extends AbstractAuthenticationToken> converter = cfg.jwtAuthenticationConverter();

        Jwt jwt = jwtWithClaims(Map.of(
                "sub", "a@example.com",
                "roles", List.of("ROLE_USER", "ROLE_SUPERVISOR")
        ));

        JwtAuthenticationToken token = (JwtAuthenticationToken) converter.convert(jwt);
        Set<String> authorities = token.getAuthorities().stream().map(a -> a.getAuthority()).collect(Collectors.toSet());

        assertThat(authorities).contains("ROLE_USER", "ROLE_SUPERVISOR");
    }

    @Test
    void jwtAuthenticationConverter_shouldExtractAuthoritiesFromCommaSeparatedString() {
        SecurityConfig cfg = new SecurityConfig();
        Converter<Jwt, ? extends AbstractAuthenticationToken> converter = cfg.jwtAuthenticationConverter();

        Jwt jwt = jwtWithClaims(Map.of(
                "sub", "a@example.com",
                "roles", " ROLE_USER,  ROLE_SUPERVISOR ,,"
        ));

        JwtAuthenticationToken token = (JwtAuthenticationToken) converter.convert(jwt);
        assertThat(token.getAuthorities()).containsExactlyInAnyOrder(
                new SimpleGrantedAuthority("ROLE_USER"),
                new SimpleGrantedAuthority("ROLE_SUPERVISOR")
        );
    }

    @Test
    void jwtAuthenticationConverter_shouldReturnEmptyAuthorities_whenRolesMissingOrBlank() {
        SecurityConfig cfg = new SecurityConfig();
        Converter<Jwt, ? extends AbstractAuthenticationToken> converter = cfg.jwtAuthenticationConverter();

        Jwt missing = jwtWithClaims(Map.of("sub", "a@example.com"));
        Jwt blank = jwtWithClaims(Map.of("sub", "a@example.com", "roles", "   "));

        JwtAuthenticationToken t1 = (JwtAuthenticationToken) converter.convert(missing);
        JwtAuthenticationToken t2 = (JwtAuthenticationToken) converter.convert(blank);

        assertThat(t1.getAuthorities()).isEmpty();
        assertThat(t2.getAuthorities()).isEmpty();
    }
}


