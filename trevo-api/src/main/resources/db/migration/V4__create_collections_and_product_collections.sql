-- ============================================================
-- V4: Collections and product_collections (many-to-many)
-- ============================================================

CREATE TABLE collections (
    id              BIGSERIAL       PRIMARY KEY,
    shop_id         BIGINT          NOT NULL,
    name            VARCHAR(255)    NOT NULL,
    display_order   INT             NOT NULL DEFAULT 0,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_collections_shop
        FOREIGN KEY (shop_id) REFERENCES shops (id) ON DELETE CASCADE
);

CREATE INDEX idx_collections_shop ON collections (shop_id);

CREATE TABLE product_collections (
    product_id      BIGINT          NOT NULL,
    collection_id   BIGINT          NOT NULL,

    PRIMARY KEY (product_id, collection_id),

    CONSTRAINT fk_pc_product
        FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE,

    CONSTRAINT fk_pc_collection
        FOREIGN KEY (collection_id) REFERENCES collections (id) ON DELETE CASCADE
);
