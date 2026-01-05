INSERT INTO users (email, name)
SELECT
    'user' || gs || '@example.com',
    'User ' || gs
FROM generate_series(1, 100) AS gs;
