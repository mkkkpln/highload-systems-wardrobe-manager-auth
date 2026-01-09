-- Normalize seed outfits so that each outfit references wardrobe items owned by the same user.
-- This migration ONLY rewrites data if it looks like the original seed (Outfit #1..100).
--
-- It assumes wardrobe-service seed normalization creates item ids:
--   topId    = 3*(user_id-1)+1
--   bottomId = 3*(user_id-1)+2
--   shoesId  = 3*(user_id-1)+3

DO $$
DECLARE
    outfits_count BIGINT;
    outfits_like  BIGINT;
BEGIN
    SELECT COUNT(*) INTO outfits_count FROM outfits;
    SELECT COUNT(*) INTO outfits_like FROM outfits WHERE title LIKE 'Outfit #%';

    -- Rewrite only if table looks like the original seed (exactly 100 outfits and all with Outfit #%)
    IF outfits_count = 100 AND outfits_like = 100 THEN
        TRUNCATE TABLE outfit_items;
        TRUNCATE TABLE outfits RESTART IDENTITY;

        -- Re-seed 100 outfits for users 1..100
        INSERT INTO outfits (title, user_id, created_at)
        SELECT
            'Outfit #' || gs,
            gs,
            NOW()
        FROM generate_series(1, 100) AS gs;

        -- Add 3 items per outfit, matching the owning user_id
        INSERT INTO outfit_items (outfit_id, item_id, role, position_index)
        SELECT
            o.id,
            (3 * (o.user_id - 1) + 1),
            'TOP',
            1
        FROM outfits o;

        INSERT INTO outfit_items (outfit_id, item_id, role, position_index)
        SELECT
            o.id,
            (3 * (o.user_id - 1) + 2),
            'BOTTOM',
            2
        FROM outfits o;

        INSERT INTO outfit_items (outfit_id, item_id, role, position_index)
        SELECT
            o.id,
            (3 * (o.user_id - 1) + 3),
            'SHOES',
            3
        FROM outfits o;
    END IF;
END $$;


