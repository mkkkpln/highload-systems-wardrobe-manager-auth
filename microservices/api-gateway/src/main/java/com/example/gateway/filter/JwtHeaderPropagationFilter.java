package com.example.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.stream.Collectors;

@Component
public class JwtHeaderPropagationFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return exchange.getPrincipal()
                .cast(Authentication.class)
                .flatMap(auth -> {
                    if (!(auth instanceof JwtAuthenticationToken jwtAuth)) {
                        return chain.filter(exchange);
                    }

                    Jwt jwt = jwtAuth.getToken();
                    String email = jwt.getSubject();
                    String userId = String.valueOf(jwt.getClaims().get("userId"));
                    String roles = extractRoles(jwt.getClaims().get("roles"));

                    ServerHttpRequest mutated = exchange.getRequest()
                            .mutate()
                            .header("X-User-Id", userId)
                            .header("X-User-Email", email)
                            .header("X-User-Roles", roles)
                            .build();

                    return chain.filter(exchange.mutate().request(mutated).build());
                })
                .switchIfEmpty(chain.filter(exchange));
    }

    private static String extractRoles(Object rolesClaim) {
        if (rolesClaim == null) return "";
        if (rolesClaim instanceof Collection<?> roles) {
            return roles.stream().map(Object::toString).collect(Collectors.joining(","));
        }
        return rolesClaim.toString();
    }

    @Override
    public int getOrder() {
        return -1;
    }
}


