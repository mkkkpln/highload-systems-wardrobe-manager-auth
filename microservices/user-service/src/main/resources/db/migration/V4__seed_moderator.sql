-- Ensure a moderator exists (password: 'password')
INSERT INTO users (email, name, password_hash, role)
SELECT 'moderator@example.com', 'Moderator', '$2y$10$4M0/LzymhoqEUUPUruSGJ.STlI6ie09KELhT.iw4OjXJeiO2vWI8G', 'ROLE_MODERATOR'
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'moderator@example.com');


