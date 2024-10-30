-- V1__initial_schema.sql

-- Table for User
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(255) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    verification_code VARCHAR(64),
    login_attempts INTEGER NOT NULL DEFAULT 0,
    account_locked BOOLEAN NOT NULL DEFAULT FALSE,
    enabled BOOLEAN DEFAULT FALSE,
    reset_password_token VARCHAR(255),
    reset_password_expiry TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Table for Roles associated with each User
CREATE TABLE roles (
    user_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    role VARCHAR(255) NOT NULL,
    PRIMARY KEY (user_id, role)
);

-- Table for Audit Logs
CREATE TABLE audit_logs (
    id BIGSERIAL PRIMARY KEY,
    timestamp TIMESTAMP NOT NULL,
    level VARCHAR(10) NOT NULL,
    logger VARCHAR(255) NOT NULL,
    message VARCHAR(5000) NOT NULL,
    thread VARCHAR(255),
    exception TEXT
);

-- Table for Blacklisted Tokens
CREATE TABLE blacklisted_tokens (
    id BIGSERIAL PRIMARY KEY,
    token VARCHAR(255) UNIQUE NOT NULL,
    expiration_date TIMESTAMP NOT NULL
);

-- Table for Refresh Tokens
CREATE TABLE refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    token VARCHAR(255) UNIQUE NOT NULL,
    user_id BIGINT UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    expiry_date TIMESTAMP NOT NULL
);
