package com.example.userservice.service;

import com.example.userservice.dto.UserDto;
import com.example.userservice.dto.UserResponseDto;
import com.example.userservice.entity.User;
import com.example.userservice.exception.NotFoundException;
import com.example.userservice.mapper.UserMapper;
import com.example.userservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private UserResponseDto testUserDto;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setName("Test User");

        testUserDto = new UserResponseDto(1L, "test@example.com", "Test User");
    }

    @Test
    void getAll_shouldReturnAllUsers() {
        // Given
        List<User> users = List.of(testUser);
        List<UserResponseDto> userDtos = List.of(testUserDto);

        when(userRepository.findAll()).thenReturn(users);
        when(userMapper.toDto(any(User.class))).thenReturn(testUserDto);

        // When
        List<UserResponseDto> result = userService.getAll();

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(1L);
        assertThat(result.get(0).email()).isEqualTo("test@example.com");

        verify(userRepository).findAll();
        verify(userMapper).toDto(testUser);
    }

    @Test
    void getById_shouldReturnUser_whenExists() {
        // Given
        Long id = 1L;
        when(userRepository.findById(id)).thenReturn(Optional.of(testUser));
        when(userMapper.toDto(testUser)).thenReturn(testUserDto);

        // When
        UserResponseDto result = userService.getById(id);

        // Then
        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.email()).isEqualTo("test@example.com");
        assertThat(result.name()).isEqualTo("Test User");

        verify(userRepository).findById(id);
        verify(userMapper).toDto(testUser);
    }

    @Test
    void getById_shouldThrowNotFoundException_whenNotExists() {
        // Given
        Long id = 999L;
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.getById(id))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("User not found with id: 999");

        verify(userRepository).findById(id);
        verify(userMapper, never()).toDto(any());
    }

    @Test
    void create_shouldCreateUser_whenEmailDoesNotExist() {
        // Given
        UserDto createDto = new UserDto("new@example.com", "New User");
        User newUser = new User();
        newUser.setEmail("new@example.com");
        newUser.setName("New User");
        User savedUser = new User();
        savedUser.setId(1L);
        savedUser.setEmail("new@example.com");
        savedUser.setName("New User");
        UserResponseDto savedDto = new UserResponseDto(1L, "new@example.com", "New User");

        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(userMapper.toEntity(createDto)).thenReturn(newUser);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(userMapper.toDto(savedUser)).thenReturn(savedDto);

        // When
        UserResponseDto result = userService.create(createDto);

        // Then
        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.email()).isEqualTo("new@example.com");
        assertThat(result.name()).isEqualTo("New User");

        verify(userRepository).existsByEmail("new@example.com");
        verify(userMapper).toEntity(createDto);
        verify(userRepository).save(any(User.class));
        verify(userMapper).toDto(savedUser);
    }

    @Test
    void create_shouldThrowException_whenEmailAlreadyExists() {
        // Given
        UserDto createDto = new UserDto("existing@example.com", "Existing User");
        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> userService.create(createDto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User with email already exists: existing@example.com");

        verify(userRepository).existsByEmail("existing@example.com");
        verify(userRepository, never()).save(any());
        verify(userMapper, never()).toEntity(any());
    }

    @Test
    void update_shouldUpdateUser_whenExistsAndEmailUnique() {
        // Given
        Long id = 1L;
        UserDto updateDto = new UserDto("updated@example.com", "Updated User");
        UserResponseDto updatedDto = new UserResponseDto(1L, "updated@example.com", "Updated User");

        when(userRepository.findById(id)).thenReturn(Optional.of(testUser));
        when(userRepository.existsByEmail("updated@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(userMapper.toDto(any(User.class))).thenReturn(updatedDto);

        // When
        UserResponseDto result = userService.update(id, updateDto);

        // Then
        assertThat(result.email()).isEqualTo("updated@example.com");
        assertThat(result.name()).isEqualTo("Updated User");

        verify(userRepository).findById(id);
        verify(userRepository).existsByEmail("updated@example.com");
        verify(userMapper).updateEntityFromDto(updateDto, testUser);
        verify(userRepository).save(testUser);
        verify(userMapper).toDto(testUser);
    }

    @Test
    void update_shouldThrowNotFoundException_whenUserNotExists() {
        // Given
        Long id = 999L;
        UserDto updateDto = new UserDto( "test@example.com", "Test User");
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.update(id, updateDto))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("User not found with id: 999");

        verify(userRepository).findById(id);
        verify(userRepository, never()).existsByEmail(anyString());
        verify(userRepository, never()).save(any());
    }

    @Test
    void update_shouldThrowException_whenEmailAlreadyExists() {
        // Given
        Long id = 1L;
        UserDto updateDto = new UserDto("existing@example.com", "Updated User");
        when(userRepository.findById(id)).thenReturn(Optional.of(testUser));
        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> userService.update(id, updateDto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User with email already exists: existing@example.com");

        verify(userRepository).findById(id);
        verify(userRepository).existsByEmail("existing@example.com");
        verify(userRepository, never()).save(any());
    }

    @Test
    void update_shouldAllowSameEmail_whenUpdatingSameUser() {
        // Given
        Long id = 1L;
        UserDto updateDto = new UserDto("test@example.com", "Updated Name");
        UserResponseDto updatedDto = new UserResponseDto(1L, "test@example.com", "Updated Name");

        when(userRepository.findById(id)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(userMapper.toDto(any(User.class))).thenReturn(updatedDto);

        // When
        UserResponseDto result = userService.update(id, updateDto);

        // Then
        assertThat(result.name()).isEqualTo("Updated Name");
        assertThat(result.email()).isEqualTo("test@example.com");

        verify(userRepository).findById(id);
        verify(userRepository, never()).existsByEmail(anyString()); // Email не изменился
        verify(userMapper).updateEntityFromDto(updateDto, testUser);
        verify(userRepository).save(testUser);
    }

    @Test
    void delete_shouldDeleteUser_whenExists() {
        // Given
        Long id = 1L;
        when(userRepository.existsById(id)).thenReturn(true);
        doNothing().when(userRepository).deleteById(id);

        // When
        userService.delete(id);

        // Then
        verify(userRepository).existsById(id);
        verify(userRepository).deleteById(id);
    }

    @Test
    void delete_shouldThrowNotFoundException_whenNotExists() {
        // Given
        Long id = 999L;
        when(userRepository.existsById(id)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> userService.delete(id))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("User not found with id: 999");

        verify(userRepository).existsById(id);
        verify(userRepository, never()).deleteById(anyLong());
    }
}
