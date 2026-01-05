-- Seed 100 outfits

INSERT INTO outfits (title, user_id, created_at)
SELECT
    'Outfit #' || gs,
    (gs % 100) + 1,
    NOW()
FROM generate_series(1, 100) AS gs;

-- Seed outfit items (2 items per outfit)

INSERT INTO outfit_items (
    outfit_id,
    item_id,
    role,
    position_index
)
SELECT
    o.id,
    ((o.id * 2) % 100) + 1,
    'TOP',
    1
FROM outfits o;

INSERT INTO outfit_items (
    outfit_id,
    item_id,
    role,
    position_index
)
SELECT
    o.id,
    ((o.id * 2 + 1) % 100) + 1,
    'BOTTOM',
    2
FROM outfits o;
