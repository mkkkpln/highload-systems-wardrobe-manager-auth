package com.example.outfitservice.entity;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "outfit_items")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString
public class OutfitItem {

    @EmbeddedId
    @EqualsAndHashCode.Include
    private OutfitItemId id = new OutfitItemId();

    @ToString.Exclude
    @MapsId("outfitId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "outfit_id", nullable = false)
    private Outfit outfit;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", length = 50, nullable = false)
    private OutfitRole role;

    @Column(name = "position_index", nullable = false)
    private int positionIndex;

    // Helper methods to access ID components
    public Long getOutfitId() {
        return id != null ? id.getOutfitId() : null;
    }

    public void setOutfitId(Long outfitId) {
        if (id == null) {
            id = new OutfitItemId();
        }
        id.setOutfitId(outfitId);
    }

    public Long getItemId() {
        return id != null ? id.getItemId() : null;
    }

    public void setItemId(Long itemId) {
        if (id == null) {
            id = new OutfitItemId();
        }
        id.setItemId(itemId);
    }
}
