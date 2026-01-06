package com.example.userservice.service;

import com.example.userservice.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class JwtTokenService {

    private final JwtEncoder jwtEncoder;

    @Value("${jwt.ttl-seconds:3600}")
    private long ttlSeconds;

    public String issueAccessToken(User user) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(ttlSeconds);

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuedAt(now)
                .expiresAt(exp)
                .subject(user.getEmail())
                .claim("userId", String.valueOf(user.getId()))
                .claim("roles", List.of(user.getRole().name()))
                .build();

        var headers = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(
                JwtEncoderParameters.from(headers, claims)
        ).getTokenValue();
    }

    public long getTtlSeconds() {
        return ttlSeconds;
    }
}


