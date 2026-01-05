-- Initialize separate databases for each microservice
-- This script runs when PostgreSQL container starts for the first time

-- Create database for user-service
CREATE DATABASE users_db;

-- Create database for wardrobe-service
CREATE DATABASE wardrobe_db;

-- Create database for outfit-service
CREATE DATABASE outfits_db;

-- Grant all privileges on databases to the default user
-- Note: The default user is set by POSTGRES_USER environment variable
-- In docker-compose.yml this is set to 'wardrobe'

-- Set default schema search path for each database
-- This ensures our tables are created in the default schema
\c users_db;
ALTER DATABASE users_db SET search_path TO public;

\c wardrobe_db;
ALTER DATABASE wardrobe_db SET search_path TO public;

\c outfits_db;
ALTER DATABASE outfits_db SET search_path TO public;

-- Enable necessary extensions for each database
\c users_db;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

\c wardrobe_db;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

\c outfits_db;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Log initialization completion
DO $$
BEGIN
    RAISE NOTICE 'Microservices databases initialized successfully';
    RAISE NOTICE 'Created databases: users_db, wardrobe_db, outfits_db';
END
$$;
