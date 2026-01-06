package com.example.userservice.integration;

import com.example.userservice.UserServiceApplication;
import com.example.userservice.dto.UserDto;
import com.example.userservice.dto.UserResponseDto;
import com.example.userservice.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.test.annotation.DirtiesContext;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = UserServiceApplication.class)
@ActiveProfiles("test")
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@SuppressWarnings("resource")
public class UserServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("users_test")
            .withUsername("user")
            .withPassword("user");

    @Autowired
    private UserService userService;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @Test
    void shouldCreateUser() {
        // Given
        UserDto createDto = new UserDto("john@example.com", "John Doe");

        // When
        UserResponseDto created = userService.create(createDto);

        // Then

        assertThat(created.name()).isEqualTo("John Doe");
        assertThat(created.email()).isEqualTo("john@example.com");
    }

    @Test
    void shouldFailCreateUser_whenEmailAlreadyExists() {
        // First create a user
        UserDto createDto = new UserDto( "duplicate@example.com", "First User");
        userService.create(createDto);

        // Try to create another with same email
        UserDto duplicateDto = new UserDto("duplicate@example.com", "Second User");

        // When & Then
        assertThatThrownBy(() -> userService.create(duplicateDto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User with email already exists");
    }

    @Test
    void shouldGetUserById_whenExists() {
        // First create a user
        UserDto createDto = new UserDto("find@example.com", "Find User");
        UserResponseDto created = userService.create(createDto);

        // Then get it by id
        UserResponseDto found = userService.getById(created.id());

        assertThat(found.id()).isEqualTo(created.id());
        assertThat(found.email()).isEqualTo("find@example.com");
        assertThat(found.name()).isEqualTo("Find User");
    }

    @Test
    void shouldThrowNotFoundException_whenUserDoesNotExist() {
        assertThatThrownBy(() -> userService.getById(999L))
                .isInstanceOf(com.example.userservice.exception.NotFoundException.class)
                .hasMessageContaining("User not found with id: 999");
    }

    @Test
    void shouldGetAllUsers() {
        // Create multiple users
        userService.create(new UserDto("user1@example.com", "User 1"));
        userService.create(new UserDto("user2@example.com", "User 2"));
        userService.create(new UserDto("user3@example.com", "User 3"));

        // Get all users
        List<UserResponseDto> users = userService.getAll();

        assertThat(users).hasSizeGreaterThanOrEqualTo(3);
        assertThat(users.stream().anyMatch(u -> u.email().equals("user1@example.com"))).isTrue();
        assertThat(users.stream().anyMatch(u -> u.email().equals("user2@example.com"))).isTrue();
        assertThat(users.stream().anyMatch(u -> u.email().equals("user3@example.com"))).isTrue();
    }

    @Test
    void shouldUpdateUser_whenExists() {
        // First create a user
        UserDto createDto = new UserDto("update@example.com", "Original Name");
        UserResponseDto created = userService.create(createDto);

        // Then update it
        UserDto updateDto = new UserDto("update@example.com", "Updated Name");
        UserResponseDto updated = userService.update(created.id(), updateDto);

        assertThat(updated.id()).isEqualTo(created.id());
        assertThat(updated.email()).isEqualTo("update@example.com");
        assertThat(updated.name()).isEqualTo("Updated Name");
    }

    @Test
    void shouldFailUpdateUser_whenEmailAlreadyExists() {
        // given
        UserResponseDto user1 = userService.create(
                new UserDto("user1@example.com", "User 1")
        );
        userService.create(
                new UserDto( "user2@example.com", "User 2")
        );

        UserDto updateDto = new UserDto(
                "user2@example.com",
                "Updated User 1"
        );

        // when + then
        assertThatThrownBy(() -> userService.update(user1.id(), updateDto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User with email already exists");
    }


    @Test
    void shouldAllowUpdateUser_withSameEmail() {
        // Create a user
        UserResponseDto created = userService.create(new UserDto("same@example.com", "Original Name"));

        // Update with same email but different name
        UserDto updateDto = new UserDto("same@example.com", "Updated Name");
        UserResponseDto updated = userService.update(created.id(), updateDto);

        assertThat(updated.email()).isEqualTo("same@example.com");
        assertThat(updated.name()).isEqualTo("Updated Name");
    }

    @Test
    void shouldDeleteUser_whenExists() {
        // First create a user
        UserResponseDto created = userService.create(new UserDto("delete@example.com", "Delete User"));

        // Then delete it
        userService.delete(created.id());

        // Verify it's deleted
        assertThatThrownBy(() -> userService.getById(created.id()))
                .isInstanceOf(com.example.userservice.exception.NotFoundException.class);
    }

    @Test
    void shouldThrowNotFoundException_whenDeletingNonExistentUser() {
        assertThatThrownBy(() -> userService.delete(999L))
                .isInstanceOf(com.example.userservice.exception.NotFoundException.class)
                .hasMessageContaining("User not found with id: 999");
    }
}
