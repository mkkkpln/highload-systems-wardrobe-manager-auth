package com.example.wardrobeservice.service;

import java.util.List;

public record PagedResult<T>(
        List<T> content,
        long totalElements
) {
}
