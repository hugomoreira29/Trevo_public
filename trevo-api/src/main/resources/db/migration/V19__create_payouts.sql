-- ============================================================
-- V19: Payouts — escrow release records created on DELIVERED
-- ============================================================

CREATE TABLE payouts (
    id              BIGSERIAL       PRIMARY KEY,
    order_id        BIGINT          NOT NULL UNIQUE,
    payout_amount   DECIMAL(10, 2)  NOT NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_payouts_order
        FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE RESTRICT,

    CONSTRAINT chk_payout_amount_positive
        CHECK (payout_amount > 0)
);

CREATE INDEX idx_payouts_order ON payouts (order_id);
