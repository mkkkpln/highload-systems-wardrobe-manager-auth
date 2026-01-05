package com.example.wardrobeservice.client;

import com.example.wardrobeservice.dto.UserDto;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class UserServiceClientTest {

    private MockWebServer mockWebServer;
    private UserServiceClient userServiceClient;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        String baseUrl = "http://localhost:" + mockWebServer.getPort();
        WebClient.Builder webClientBuilder = WebClient.builder().baseUrl(baseUrl);

        // prod-конструктор + подменённый base-url
        userServiceClient = new UserServiceClient(webClientBuilder, baseUrl);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void getUserById_shouldReturnUser_whenSuccessful() {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody("""
                        {
                          "id": 1,
                          "email": "test@example.com",
                          "name": "Test User"
                        }
                        """));

        Mono<UserDto> result = userServiceClient.getUserById(1L);

        StepVerifier.create(result)
                .assertNext(user -> {
                    assertThat(user.id()).isEqualTo(1L);
                    assertThat(user.email()).isEqualTo("test@example.com");
                    assertThat(user.name()).isEqualTo("Test User");
                })
                .verifyComplete();
    }

    @Test
    void getUserById_shouldError_when404() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(404));

        StepVerifier.create(userServiceClient.getUserById(999L))
                .expectError()
                .verify();
    }

    @Test
    void getUserById_shouldError_when500() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(500));

        StepVerifier.create(userServiceClient.getUserById(1L))
                .expectError()
                .verify();
    }
}
