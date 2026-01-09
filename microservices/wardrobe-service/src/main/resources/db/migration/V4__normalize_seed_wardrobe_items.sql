-- Normalize seed data so that outfits can safely reference items owned by the same user.
-- This migration ONLY rewrites data if it looks like the original seed (Brand 1..100).
--
-- New deterministic scheme:
-- For each user u in 1..100 create 3 items with ids:
--   topId    = 3*(u-1)+1
--   bottomId = 3*(u-1)+2
--   shoesId  = 3*(u-1)+3
-- and owner_id = u for all three.

DO $$
DECLARE
    seed_count BIGINT;
    seed_like  BIGINT;
BEGIN
    SELECT COUNT(*) INTO seed_count FROM wardrobe_items;
    SELECT COUNT(*) INTO seed_like FROM wardrobe_items WHERE brand LIKE 'Brand %';

    -- Rewrite only if table looks like the original seed (exactly 100 items and all with Brand %)
    IF seed_count = 100 AND seed_like = 100 THEN
        TRUNCATE TABLE wardrobe_items RESTART IDENTITY;

        INSERT INTO wardrobe_items (id, owner_id, type, brand, color, season, image_url, created_at)
        SELECT
            gs AS id,
            ((gs - 1) / 3) + 1 AS owner_id, -- users 1..100
            CASE (gs % 3)
                WHEN 1 THEN 'SHIRT'
                WHEN 2 THEN 'PANTS'
                ELSE 'SHOES'
            END AS type,
            'SeedBrand ' || gs AS brand,
            (ARRAY['black','white','blue','red','green'])[1 + (gs % 5)] AS color,
            (ARRAY['WINTER','SPRING','SUMMER','AUTUMN','ALL_SEASONS'])[1 + (gs % 5)] AS season,
            'https://img.example.com/item-' || gs || '.jpg' AS image_url,
            NOW() AS created_at
        FROM generate_series(1, 300) AS gs;
    END IF;
END $$;


