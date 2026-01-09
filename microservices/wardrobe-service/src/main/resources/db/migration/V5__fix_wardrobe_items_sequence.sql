-- Fix wardrobe_items id sequence after seed migrations that insert explicit ids.
-- Without this, new inserts may reuse existing ids and fail with duplicate key violations.
SELECT setval('wardrobe_items_id_seq', COALESCE((SELECT MAX(id) FROM wardrobe_items), 1), true);


