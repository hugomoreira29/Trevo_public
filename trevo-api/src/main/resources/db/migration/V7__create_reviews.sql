-- ============================================================
-- V7: Reviews
-- ============================================================

CREATE TABLE reviews (
    id              BIGSERIAL       PRIMARY KEY,
    buyer_id        BIGINT          NOT NULL,
    shop_id         BIGINT          NOT NULL,
    rating          INT             NOT NULL,
    comment         TEXT,
    response        TEXT,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP       NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_reviews_buyer
        FOREIGN KEY (buyer_id) REFERENCES users (id) ON DELETE CASCADE,

    CONSTRAINT fk_reviews_shop
        FOREIGN KEY (shop_id) REFERENCES shops (id) ON DELETE CASCADE,

    CONSTRAINT chk_rating_range
        CHECK (rating BETWEEN 1 AND 5),

    CONSTRAINT uq_review_buyer_shop
        UNIQUE (buyer_id, shop_id)
);

CREATE INDEX idx_reviews_shop ON reviews (shop_id);

-- ============================================================
-- Trigger to automatically update 'updated_at' on review modifications
-- ============================================================

CREATE OR REPLACE FUNCTION update_review_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_update_review_updated_at
BEFORE UPDATE ON reviews
FOR EACH ROW
WHEN (OLD.* IS DISTINCT FROM NEW.*)
EXECUTE FUNCTION update_review_updated_at();

