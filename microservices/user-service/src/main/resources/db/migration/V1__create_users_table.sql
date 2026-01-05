-- Users table for Wardrobe Manager microservices
-- This table stores user account information

CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

-- Create index for email lookups (used in authentication)
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);

-- Create index for name searches
CREATE INDEX IF NOT EXISTS idx_users_name ON users(name);
