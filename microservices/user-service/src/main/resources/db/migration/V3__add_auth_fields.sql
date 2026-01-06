-- Add authentication/authorization fields
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS password_hash VARCHAR(100),
    ADD COLUMN IF NOT EXISTS role VARCHAR(50);

-- Backfill existing users with default role + default password hash ('password')
UPDATE users
SET password_hash = COALESCE(password_hash, '$2y$10$4M0/LzymhoqEUUPUruSGJ.STlI6ie09KELhT.iw4OjXJeiO2vWI8G'),
    role         = COALESCE(role, 'ROLE_USER');

ALTER TABLE users
    ALTER COLUMN password_hash SET NOT NULL,
    ALTER COLUMN role SET NOT NULL;

-- Ensure a supervisor exists (password: 'password')
INSERT INTO users (email, name, password_hash, role)
SELECT 'supervisor@example.com', 'Supervisor', '$2y$10$4M0/LzymhoqEUUPUruSGJ.STlI6ie09KELhT.iw4OjXJeiO2vWI8G', 'ROLE_SUPERVISOR'
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'supervisor@example.com');


