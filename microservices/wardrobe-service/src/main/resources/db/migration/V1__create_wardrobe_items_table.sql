-- Wardrobe items table for storing clothing items
-- Each item belongs to a specific user

CREATE TABLE IF NOT EXISTS wardrobe_items (
    id BIGSERIAL PRIMARY KEY,
    owner_id BIGINT NOT NULL,
    type VARCHAR(32) NOT NULL,
    brand VARCHAR(100),
    color VARCHAR(40),
    season VARCHAR(16) NOT NULL,
    image_url VARCHAR(500) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP

    -- Foreign key constraint (will be enforced when user service is available)
    -- CONSTRAINT fk_wardrobe_items_owner FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Create indexes for efficient queries
CREATE INDEX IF NOT EXISTS idx_wardrobe_items_owner ON wardrobe_items(owner_id);
CREATE INDEX IF NOT EXISTS idx_wardrobe_items_type ON wardrobe_items(type);
CREATE INDEX IF NOT EXISTS idx_wardrobe_items_season ON wardrobe_items(season);
CREATE INDEX IF NOT EXISTS idx_wardrobe_items_created_at ON wardrobe_items(created_at DESC);

-- Composite index for common filter combinations
CREATE INDEX IF NOT EXISTS idx_wardrobe_items_owner_type ON wardrobe_items(owner_id, type);
CREATE INDEX IF NOT EXISTS idx_wardrobe_items_owner_season ON wardrobe_items(owner_id, season);
