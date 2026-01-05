package com.example.wardrobeservice.repository;

import com.example.wardrobeservice.entity.WardrobeItem;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface WardrobeItemRepository extends ReactiveCrudRepository<WardrobeItem, Long> {

    @Query("SELECT * FROM wardrobe_items ORDER BY id LIMIT :limit OFFSET :offset")
    Flux<WardrobeItem> findAllWithPagination(@Param("limit") int limit, @Param("offset") int offset);

    @Query("SELECT COUNT(*) FROM wardrobe_items")
    Mono<Long> countAll();

    Flux<WardrobeItem> findAllByOwnerId(Long ownerId);
}
