package com.example.wardrobeservice.client;

import com.example.wardrobeservice.dto.UserDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class UserServiceClient {

    private final WebClient webClient;

    public UserServiceClient(
            WebClient.Builder webClientBuilder,
            @Value("${clients.user-service.base-url:http://user-service:8081}") String baseUrl
    ) {
        this.webClient = webClientBuilder
                .baseUrl(baseUrl)
                .filter((request, next) -> ReactiveSecurityContextHolder.getContext()
                        .map(ctx -> ctx.getAuthentication())
                        .ofType(JwtAuthenticationToken.class)
                        .map(jwtAuth -> "Bearer " + jwtAuth.getToken().getTokenValue())
                        .flatMap(bearer -> {
                            ClientRequest newReq = ClientRequest.from(request)
                                    .header(HttpHeaders.AUTHORIZATION, bearer)
                                    .build();
                            return next.exchange(newReq);
                        })
                        .switchIfEmpty(next.exchange(request))
                )
                .build();
    }

    public Mono<UserDto> getUserById(Long id) {
        return webClient.get()
                .uri("/users/{id}", id)
                .retrieve()
                .bodyToMono(UserDto.class);
    }
}
