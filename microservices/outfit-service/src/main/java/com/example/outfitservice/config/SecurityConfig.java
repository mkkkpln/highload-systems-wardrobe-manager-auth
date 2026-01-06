package com.example.outfitservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtDecoder jwtDecoder,
            Converter<Jwt, ? extends AbstractAuthenticationToken> jwtAuthConverter
    ) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/actuator/**"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .jwt(jwt -> jwt
                                .decoder(jwtDecoder)
                                .jwtAuthenticationConverter(jwtAuthConverter)
                        )
                )
                .build();
    }

    @Bean
    public JwtDecoder jwtDecoder(@Value("${jwt.secret}") String base64Secret) {
        byte[] decoded = Base64.getDecoder().decode(base64Secret);
        SecretKey key = new SecretKeySpec(decoded, "HmacSHA256");
        return NimbusJwtDecoder.withSecretKey(key).build();
    }

    @Bean
    public Converter<Jwt, ? extends AbstractAuthenticationToken> jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(SecurityConfig::extractAuthorities);
        return jwt -> new JwtAuthenticationToken(jwt, converter.convert(jwt).getAuthorities(), jwt.getSubject());
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
        if (rolesStr.isBlank()) return List.of();

        return List.of(rolesStr.split(",")).stream()
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toSet());
    }
}


