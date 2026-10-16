-- ============================================================
-- V3: Products, product images, and variants
-- ============================================================

CREATE TYPE product_status AS ENUM ('ACTIVE', 'ARCHIVED');

CREATE TABLE products (
    id              BIGSERIAL       PRIMARY KEY,
    shop_id         BIGINT          NOT NULL,
    name            VARCHAR(255)    NOT NULL,
    description     TEXT            NOT NULL,
    price           DECIMAL(10, 2)  NOT NULL,
    brand           VARCHAR(255)    NOT NULL,
    materials       VARCHAR(500),
    status          product_status  NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP       NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_products_shop
        FOREIGN KEY (shop_id) REFERENCES shops (id) ON DELETE CASCADE,

    CONSTRAINT chk_price_positive
        CHECK (price > 0)
);

CREATE INDEX idx_products_shop ON products (shop_id);
CREATE INDEX idx_products_status ON products (status);
CREATE INDEX idx_products_name ON products USING GIN (to_tsvector('portuguese', name));
CREATE INDEX idx_products_description ON products USING GIN (to_tsvector('portuguese', description));

CREATE TABLE product_images (
    id              BIGSERIAL       PRIMARY KEY,
    product_id      BIGINT          NOT NULL,
    image_url       VARCHAR(512)    NOT NULL,
    display_order   INT             NOT NULL DEFAULT 0,

    CONSTRAINT fk_product_images_product
        FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE
);

CREATE INDEX idx_product_images_product ON product_images (product_id);

CREATE TABLE variants (
    id              BIGSERIAL       PRIMARY KEY,
    product_id      BIGINT          NOT NULL,
    size            VARCHAR(50)     NOT NULL,
    color           VARCHAR(50)     NOT NULL,
    stock           INT             NOT NULL DEFAULT 0,

    CONSTRAINT fk_variants_product
        FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE,

    CONSTRAINT chk_stock_non_negative
        CHECK (stock >= 0),

    CONSTRAINT uq_variant_product_size_color
        UNIQUE (product_id, size, color)
);

CREATE INDEX idx_variants_product ON variants (product_id);
CREATE INDEX idx_variants_size ON variants (size);
CREATE INDEX idx_variants_color ON variants (color);
