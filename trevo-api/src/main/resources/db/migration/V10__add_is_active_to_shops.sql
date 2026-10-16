-- ============================================================
-- V10: Add is_active field to shops table
-- ============================================================

ALTER TABLE shops ADD COLUMN is_active BOOLEAN NOT NULL DEFAULT true;

CREATE INDEX idx_shops_is_active ON shops (is_active);
