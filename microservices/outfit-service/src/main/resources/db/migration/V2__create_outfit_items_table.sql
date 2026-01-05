-- Outfit items junction table (many-to-many relationship)
-- Links outfits with wardrobe items and stores additional metadata

CREATE TABLE IF NOT EXISTS outfit_items (
    outfit_id BIGINT NOT NULL,
    item_id BIGINT NOT NULL,
    role VARCHAR(50) NOT NULL,
    position_index INTEGER NOT NULL DEFAULT 0,

    -- Composite primary key
    PRIMARY KEY (outfit_id, item_id)

    -- Foreign key constraints (will be enforced when services are available)
    -- CONSTRAINT fk_outfit_items_outfit FOREIGN KEY (outfit_id) REFERENCES outfits(id) ON DELETE CASCADE,
    -- CONSTRAINT fk_outfit_items_item FOREIGN KEY (item_id) REFERENCES wardrobe_items(id) ON DELETE CASCADE
);

-- Create indexes for efficient queries
CREATE INDEX IF NOT EXISTS idx_outfit_items_outfit ON outfit_items(outfit_id);
CREATE INDEX IF NOT EXISTS idx_outfit_items_item ON outfit_items(item_id);
CREATE INDEX IF NOT EXISTS idx_outfit_items_role ON outfit_items(role);

-- Composite index for outfit queries
CREATE INDEX IF NOT EXISTS idx_outfit_items_outfit_position ON outfit_items(outfit_id, position_index);
