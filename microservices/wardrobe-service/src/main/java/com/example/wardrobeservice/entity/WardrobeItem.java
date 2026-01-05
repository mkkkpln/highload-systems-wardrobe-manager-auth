package com.example.wardrobeservice.entity;

import com.example.wardrobeservice.entity.enums.ItemType;
import com.example.wardrobeservice.entity.enums.Season;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

@Table("wardrobe_items")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WardrobeItem {
    @Id
    private Long id;

    @Column("owner_id")
    private Long ownerId;

    @Column("type")
    private ItemType type;

    @Column("brand")
    private String brand;

    @Column("color")
    private String color;

    @Column("season")
    private Season season;

    @Column("image_url")
    private String imageUrl;

    @Column("created_at")
    private Instant createdAt;
}
