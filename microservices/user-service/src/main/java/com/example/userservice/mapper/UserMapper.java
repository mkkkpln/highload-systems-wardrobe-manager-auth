package com.example.userservice.mapper;

import com.example.userservice.dto.UserDto;
import com.example.userservice.dto.UserResponseDto;
import com.example.userservice.entity.User;

public interface UserMapper {

    UserResponseDto toDto(User user);

    User toEntity(UserDto userDto);

    void updateEntityFromDto(UserDto userDto, User user);
}
