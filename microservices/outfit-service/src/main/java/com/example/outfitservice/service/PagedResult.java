package com.example.outfitservice.service;

import java.util.List;

public record PagedResult<T>(
        List<T> items,
        long totalCount
) {
}
