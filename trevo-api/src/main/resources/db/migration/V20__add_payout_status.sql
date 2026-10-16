-- ============================================================
-- V20: Add status and released_at to payouts
-- ============================================================

ALTER TABLE payouts
    ADD COLUMN status      VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    ADD COLUMN released_at TIMESTAMP;

CREATE INDEX idx_payouts_shop_status ON payouts (status);
