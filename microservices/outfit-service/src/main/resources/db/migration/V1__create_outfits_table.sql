-- Outfits table for storing clothing combinations
-- Each outfit belongs to a specific user and contains multiple wardrobe items

CREATE TABLE IF NOT EXISTS outfits (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(120) NOT NULL,
    user_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

-- Create indexes for efficient queries
CREATE INDEX IF NOT EXISTS idx_outfits_user ON outfits(user_id);
CREATE INDEX IF NOT EXISTS idx_outfits_created_at ON outfits(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_outfits_title ON outfits(title);

-- Partial index for recent outfits (performance optimization)
-- Temporarily disabled - partial indexes may cause issues
-- CREATE INDEX IF NOT EXISTS idx_outfits_recent ON outfits(user_id, created_at DESC)
-- WHERE created_at > NOW() - INTERVAL '30 days';
