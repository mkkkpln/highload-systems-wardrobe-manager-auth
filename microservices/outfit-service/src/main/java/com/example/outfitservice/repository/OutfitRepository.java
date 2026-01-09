package com.example.outfitservice.repository;

import com.example.outfitservice.entity.Outfit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OutfitRepository extends JpaRepository<Outfit, Long> {

    Page<Outfit> findAll(Pageable pageable);

    Page<Outfit> findAllByUserId(Long userId, Pageable pageable);

    List<Outfit> findAllByUserId(Long userId);

    /**
     * Infinite scroll by "starting id": returns outfits with id >= fromId, limited by limit.
     * This matches API parameter name "offset" used by /outfits/scroll, where caller expects
     * offset=78 to start from id=78 (not rounded down to page boundary).
     */
    @Query(value = "SELECT * FROM outfits WHERE id >= :fromId ORDER BY id LIMIT :limit", nativeQuery = true)
    List<Outfit> findAllScrollFromId(@Param("fromId") long fromId, @Param("limit") int limit);

    @Query(value = "SELECT * FROM outfits WHERE user_id = :userId AND id >= :fromId ORDER BY id LIMIT :limit", nativeQuery = true)
    List<Outfit> findAllByUserIdScrollFromId(@Param("userId") long userId, @Param("fromId") long fromId, @Param("limit") int limit);

    boolean existsByIdAndUserId(Long id, Long userId);
}
