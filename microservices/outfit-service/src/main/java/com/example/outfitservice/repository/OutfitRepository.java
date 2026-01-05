package com.example.outfitservice.repository;

import com.example.outfitservice.entity.Outfit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OutfitRepository extends JpaRepository<Outfit, Long> {

    Page<Outfit> findAll(Pageable pageable);
}
