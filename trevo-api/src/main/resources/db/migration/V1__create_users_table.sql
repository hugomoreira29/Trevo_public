-- ============================================================
-- V1: Enum types + Users table
-- ============================================================

CREATE TYPE user_role AS ENUM ('BUYER', 'SHOP_OWNER');
CREATE TYPE user_status AS ENUM ('ACTIVE', 'PENDING', 'SUSPENDED');

CREATE TABLE users (
    id              BIGSERIAL       PRIMARY KEY,
    email           VARCHAR(255)    NOT NULL UNIQUE,
    password_hash   VARCHAR(255)    NOT NULL,
    full_name       VARCHAR(255)    NOT NULL,
    phone           VARCHAR(20),
    role            user_role       NOT NULL,
    status          user_status     NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_users_email ON users (email);
CREATE INDEX idx_users_role ON users (role);
