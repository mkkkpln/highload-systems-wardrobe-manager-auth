package com.example.userservice.mapper;

import com.example.userservice.dto.UserDto;
import com.example.userservice.dto.UserResponseDto;
import com.example.userservice.entity.User;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
public class UserMapperManualImpl implements UserMapper {

    @Override
    public UserResponseDto toDto(User user) {
        if (user == null) return null;
        return new UserResponseDto(user.getId(), user.getEmail(), user.getName());
    }

    @Override
    public User toEntity(UserDto userDto) {
        if (userDto == null) return null;
        User user = new User();
        user.setEmail(userDto.email());
        user.setName(userDto.name());
        return user;
    }

    @Override
    public void updateEntityFromDto(UserDto userDto, User user) {
        if (userDto == null || user == null) return;
        if (userDto.email() != null) user.setEmail(userDto.email());
        if (userDto.name() != null) user.setName(userDto.name());
    }
}


