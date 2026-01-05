-- Seed 100 wardrobe items

INSERT INTO wardrobe_items (
    owner_id,
    type,
    brand,
    color,
    season,
    image_url,
    created_at
)
SELECT
    (gs % 100) + 1, -- users 1..100
    (ARRAY['T_SHIRT','SHIRT','SWEATER','JACKET','COAT','PANTS','JEANS','DRESS','SHOES','ACCESSORY'])[1 + (gs % 10)],
    'Brand ' || gs,
    (ARRAY['black','white','blue','red','green'])[1 + (gs % 5)],
    (ARRAY['WINTER','SPRING','SUMMER','AUTUMN','ALL_SEASONS'])[1 + (gs % 5)],
    'https://img.example.com/item-' || gs || '.jpg',
    NOW()
FROM generate_series(1, 100) AS gs;
