-- PostgreSQL Initialization Script
-- Note: Database is automatically created by POSTGRES_DB environment variable

-- Enable uuid-ossp extension if ever needed (PostgreSQL 17+ has native uuidv7)
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";