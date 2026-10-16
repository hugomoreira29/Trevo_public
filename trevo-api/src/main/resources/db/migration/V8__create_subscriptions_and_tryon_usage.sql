-- ============================================================
-- V8: Subscriptions, try-on usage, try-on images, user photos
-- ============================================================

CREATE TYPE subscription_status AS ENUM ('ACTIVE', 'CANCELLED', 'EXPIRED');

CREATE TABLE subscriptions (
    id                      BIGSERIAL               PRIMARY KEY,
    buyer_id                BIGINT                  NOT NULL UNIQUE,
    status                  subscription_status     NOT NULL DEFAULT 'ACTIVE',
    stripe_subscription_id  VARCHAR(255)            UNIQUE,
    start_date              TIMESTAMP               NOT NULL DEFAULT NOW(),
    end_date                TIMESTAMP,
    created_at              TIMESTAMP               NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_subscriptions_buyer
        FOREIGN KEY (buyer_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_subscriptions_buyer ON subscriptions (buyer_id);
CREATE INDEX idx_subscriptions_status ON subscriptions (status);

CREATE TABLE tryon_usage (
    id                  BIGSERIAL       PRIMARY KEY,
    buyer_id            BIGINT          NOT NULL,
    product_id          BIGINT          NOT NULL,
    generation_count    INT             NOT NULL DEFAULT 0,

    CONSTRAINT fk_tryon_buyer
        FOREIGN KEY (buyer_id) REFERENCES users (id) ON DELETE CASCADE,

    CONSTRAINT fk_tryon_product
        FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE,

    CONSTRAINT chk_generation_count_non_negative
        CHECK (generation_count >= 0),

    CONSTRAINT uq_tryon_buyer_product
        UNIQUE (buyer_id, product_id)
);

CREATE TABLE tryon_images (
    id              BIGSERIAL       PRIMARY KEY,
    usage_id        BIGINT          NOT NULL,
    image_url       VARCHAR(512)    NOT NULL,
    generated_at    TIMESTAMP       NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_tryon_images_usage
        FOREIGN KEY (usage_id) REFERENCES tryon_usage (id) ON DELETE CASCADE
);

CREATE INDEX idx_tryon_images_usage ON tryon_images (usage_id);

CREATE TABLE user_photos (
    id              BIGSERIAL       PRIMARY KEY,
    buyer_id        BIGINT          NOT NULL,
    photo_url       VARCHAR(512)    NOT NULL,
    uploaded_at     TIMESTAMP       NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_user_photos_buyer
        FOREIGN KEY (buyer_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_user_photos_buyer ON user_photos (buyer_id);
