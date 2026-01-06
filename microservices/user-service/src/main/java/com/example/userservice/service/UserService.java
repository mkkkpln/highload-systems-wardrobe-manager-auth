package com.example.userservice.service;

import com.example.userservice.dto.UserDto;
import com.example.userservice.dto.UserResponseDto;
import com.example.userservice.entity.User;
import com.example.userservice.exception.NotFoundException;
import com.example.userservice.mapper.UserMapper;
import com.example.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public List<UserResponseDto> getAll() {
        return userRepository.findAll().stream()
                .map(userMapper::toDto)
                .collect(Collectors.toList());
    }

    public UserResponseDto getById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found with id: " + id));
        return userMapper.toDto(user);
    }

    @Transactional
    public UserResponseDto create(UserDto userDto) {
        if (userRepository.existsByEmail(userDto.email())) {
            throw new IllegalArgumentException("User with email already exists: " + userDto.email());
        }

        User user = userMapper.toEntity(userDto);
        // Legacy create (without password) -> set a random password hash so DB never stores plain text.
        user.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));
        user = userRepository.save(user);
        return userMapper.toDto(user);
    }

    @Transactional
    public UserResponseDto update(Long id, UserDto userDto) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found with id: " + id));

        // Check if email is being changed and if it's already taken
        if (!user.getEmail().equals(userDto.email()) && userRepository.existsByEmail(userDto.email())) {
            throw new IllegalArgumentException("User with email already exists: " + userDto.email());
        }

        userMapper.updateEntityFromDto(userDto, user);
        user = userRepository.save(user);
        return userMapper.toDto(user);
    }

    @Transactional
    public void delete(Long id) {
        if (!userRepository.existsById(id)) {
            throw new NotFoundException("User not found with id: " + id);
        }
        userRepository.deleteById(id);
    }
}
