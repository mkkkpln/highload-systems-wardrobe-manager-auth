-- Additional performance indexes for Wardrobe Manager
-- These indexes optimize common query patterns

-- Wardrobe items advanced indexes
CREATE INDEX IF NOT EXISTS idx_wardrobe_items_brand ON wardrobe_items(brand) WHERE brand IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_wardrobe_items_color ON wardrobe_items(color) WHERE color IS NOT NULL;

-- Composite indexes for complex queries
CREATE INDEX IF NOT EXISTS idx_wardrobe_items_filter ON wardrobe_items(owner_id, type, season, created_at DESC);

-- Full-text search indexes (for future search functionality)
-- CREATE INDEX IF NOT EXISTS idx_wardrobe_items_fulltext ON wardrobe_items
-- USING gin (to_tsvector('english', COALESCE(brand, '') || ' ' || COALESCE(color, '') || ' ' || type));

-- Partial indexes for active data
CREATE INDEX IF NOT EXISTS idx_wardrobe_items_active ON wardrobe_items(id, owner_id, type) WHERE id > 0;
