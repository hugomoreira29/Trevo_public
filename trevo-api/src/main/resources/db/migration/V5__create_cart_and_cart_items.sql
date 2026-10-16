-- ============================================================
-- V5: Cart and cart items
-- ============================================================

CREATE TABLE carts (
    id              BIGSERIAL       PRIMARY KEY,
    buyer_id        BIGINT          NOT NULL UNIQUE,

    CONSTRAINT fk_carts_buyer
        FOREIGN KEY (buyer_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE TABLE cart_items (
    id              BIGSERIAL       PRIMARY KEY,
    cart_id         BIGINT          NOT NULL,
    variant_id      BIGINT          NOT NULL,
    quantity        INT             NOT NULL,

    CONSTRAINT fk_cart_items_cart
        FOREIGN KEY (cart_id) REFERENCES carts (id) ON DELETE CASCADE,

    CONSTRAINT fk_cart_items_variant
        FOREIGN KEY (variant_id) REFERENCES variants (id) ON DELETE RESTRICT,

    CONSTRAINT chk_cart_quantity_positive
        CHECK (quantity > 0),

    CONSTRAINT uq_cart_variant
        UNIQUE (cart_id, variant_id)
);
