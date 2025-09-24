-- Create database if not exists
CREATE DATABASE IF NOT EXISTS isperp CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Create user if not exists
CREATE USER IF NOT EXISTS 'isperp'@'%' IDENTIFIED BY 'root';

-- Grant privileges
GRANT ALL PRIVILEGES ON isperp.* TO 'isperp'@'%';

-- Flush privileges
FLUSH PRIVILEGES;

-- Use the database
USE isperp;