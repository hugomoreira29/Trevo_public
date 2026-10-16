-- ============================================================
-- V6: Orders and order items
-- ============================================================

CREATE TYPE order_status AS ENUM ('PLACED', 'CONFIRMED', 'SHIPPED', 'DELIVERED', 'RETURNED', 'CANCELLED');
CREATE TYPE payment_method AS ENUM ('STRIPE', 'PAYPAL', 'MBWAY');

CREATE TABLE orders (
    id                  BIGSERIAL           PRIMARY KEY,
    buyer_id            BIGINT              NOT NULL,
    shop_id             BIGINT              NOT NULL,
    status              order_status        NOT NULL DEFAULT 'PLACED',
    tracking_number     VARCHAR(255),
    total_amount        DECIMAL(10, 2)      NOT NULL,
    commission_amount   DECIMAL(10, 2)      NOT NULL DEFAULT 0,
    payment_method      payment_method      NOT NULL,
    payment_reference   VARCHAR(255),
    created_at          TIMESTAMP           NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP           NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_orders_buyer
        FOREIGN KEY (buyer_id) REFERENCES users (id) ON DELETE RESTRICT,

    CONSTRAINT fk_orders_shop
        FOREIGN KEY (shop_id) REFERENCES shops (id) ON DELETE RESTRICT,

    CONSTRAINT chk_total_positive
        CHECK (total_amount > 0),

    CONSTRAINT chk_commission_non_negative
        CHECK (commission_amount >= 0)
);

CREATE INDEX idx_orders_buyer ON orders (buyer_id);
CREATE INDEX idx_orders_shop ON orders (shop_id);
CREATE INDEX idx_orders_status ON orders (status);
CREATE INDEX idx_orders_created ON orders (created_at DESC);

CREATE TABLE order_items (
    id              BIGSERIAL       PRIMARY KEY,
    order_id        BIGINT          NOT NULL,
    variant_id      BIGINT          NOT NULL,
    quantity        INT             NOT NULL,
    unit_price      DECIMAL(10, 2)  NOT NULL,

    CONSTRAINT fk_order_items_order
        FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE,

    CONSTRAINT fk_order_items_variant
        FOREIGN KEY (variant_id) REFERENCES variants (id) ON DELETE RESTRICT,

    CONSTRAINT chk_order_quantity_positive
        CHECK (quantity > 0),

    CONSTRAINT chk_unit_price_positive
        CHECK (unit_price > 0)
);

CREATE INDEX idx_order_items_order ON order_items (order_id);

-- ============================================================
-- Trigger to automatically update 'updated_at' on order modifications
-- ============================================================

CREATE OR REPLACE FUNCTION update_order_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_update_order_updated_at
BEFORE UPDATE ON orders
FOR EACH ROW
WHEN (OLD.* IS DISTINCT FROM NEW.*)
EXECUTE FUNCTION update_order_updated_at();

