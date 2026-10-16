-- ============================================================
-- V2: Shops table
-- ============================================================

CREATE TYPE shop_status AS ENUM ('PENDING', 'ACTIVE', 'DEACTIVATED');

CREATE TABLE shops (
    id              BIGSERIAL       PRIMARY KEY,
    owner_id        BIGINT          NOT NULL UNIQUE,
    name            VARCHAR(255)    NOT NULL,
    description     TEXT,
    logo_url        VARCHAR(512),
    banner_url      VARCHAR(512),
    nif             VARCHAR(9)      NOT NULL UNIQUE,
    status          shop_status     NOT NULL DEFAULT 'PENDING',
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP       NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_shops_owner
        FOREIGN KEY (owner_id) REFERENCES users (id) ON DELETE CASCADE,

    CONSTRAINT chk_nif_format
        CHECK (nif ~ '^\d{9}$')
);

CREATE INDEX idx_shops_status ON shops (status);
