package com.example.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(
            ServerHttpSecurity http,
            ReactiveJwtDecoder jwtDecoder,
            Converter<Jwt, Mono<AbstractAuthenticationToken>> jwtAuthenticationConverter
    ) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(auth -> auth
                        .pathMatchers(
                                "/users/auth/login",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/actuator/**"
                        ).permitAll()
                        .anyExchange().authenticated()
                )
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .jwtDecoder(jwtDecoder)
                                .jwtAuthenticationConverter(jwtAuthenticationConverter)
                        )
                )
                .build();
    }

    @Bean
    public ReactiveJwtDecoder jwtDecoder(@Value("${jwt.secret}") String base64Secret) {
        byte[] decoded = Base64.getDecoder().decode(base64Secret);
        SecretKey key = new SecretKeySpec(decoded, "HmacSHA256");
        return NimbusReactiveJwtDecoder.withSecretKey(key).build();
    }

    @Bean
    public Converter<Jwt, Mono<AbstractAuthenticationToken>> jwtAuthenticationConverter() {
        ReactiveJwtAuthenticationConverter converter = new ReactiveJwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> Flux.fromIterable(extractAuthorities(jwt)));
        return jwt -> converter.convert(jwt).map(auth -> new JwtAuthenticationToken(jwt, auth.getAuthorities(), jwt.getSubject()));
    }

    private static Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
        Object rolesClaim = jwt.getClaims().get("roles");
        if (rolesClaim == null) {
            return List.of();
        }

        if (rolesClaim instanceof Collection<?> roles) {
            return roles.stream()
                    .filter(Objects::nonNull)
                    .map(Object::toString)
                    .filter(s -> !s.isBlank())
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toSet());
        }

        String rolesStr = rolesClaim.toString();
        if (rolesStr.isBlank()) {
            return List.of();
        }

        return List.of(rolesStr.split(",")).stream()
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toSet());
    }
}


