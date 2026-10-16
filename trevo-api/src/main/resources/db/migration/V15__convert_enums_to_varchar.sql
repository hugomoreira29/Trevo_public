-- ============================================================
-- V15: Convert custom PostgreSQL enum types to VARCHAR
--
-- Custom enum types cause type-mismatch errors when Hibernate
-- binds parameters as character varying. Converting to VARCHAR
-- with CHECK constraints preserves validation without requiring
-- custom Hibernate UserTypes or implicit casts.
-- ============================================================

-- Drop V14 casts and functions (they depend on the types below)
DROP CAST IF EXISTS (character varying AS user_role);
DROP CAST IF EXISTS (character varying AS user_status);
DROP CAST IF EXISTS (character varying AS shop_status);
DROP CAST IF EXISTS (character varying AS product_status);
DROP CAST IF EXISTS (character varying AS order_status);
DROP CAST IF EXISTS (character varying AS payment_method);
DROP CAST IF EXISTS (character varying AS subscription_status);

DROP FUNCTION IF EXISTS cast_varchar_to_user_role(varchar);
DROP FUNCTION IF EXISTS cast_varchar_to_user_status(varchar);
DROP FUNCTION IF EXISTS cast_varchar_to_shop_status(varchar);
DROP FUNCTION IF EXISTS cast_varchar_to_product_status(varchar);
DROP FUNCTION IF EXISTS cast_varchar_to_order_status(varchar);
DROP FUNCTION IF EXISTS cast_varchar_to_payment_method(varchar);
DROP FUNCTION IF EXISTS cast_varchar_to_subscription_status(varchar);

-- users
ALTER TABLE users
    ALTER COLUMN role   TYPE VARCHAR(50) USING role::text,
    ALTER COLUMN status TYPE VARCHAR(50) USING status::text;

ALTER TABLE users
    ADD CONSTRAINT chk_user_role   CHECK (role   IN ('BUYER', 'SHOP_OWNER', 'ADMIN')),
    ADD CONSTRAINT chk_user_status CHECK (status IN ('ACTIVE', 'PENDING', 'SUSPENDED'));

-- shops
ALTER TABLE shops
    ALTER COLUMN status TYPE VARCHAR(50) USING status::text;

ALTER TABLE shops
    ADD CONSTRAINT chk_shop_status CHECK (status IN ('PENDING', 'ACTIVE', 'DEACTIVATED'));

-- products
ALTER TABLE products
    ALTER COLUMN status TYPE VARCHAR(50) USING status::text;

ALTER TABLE products
    ADD CONSTRAINT chk_product_status CHECK (status IN ('ACTIVE', 'ARCHIVED'));

-- orders
ALTER TABLE orders
    ALTER COLUMN status         TYPE VARCHAR(50) USING status::text,
    ALTER COLUMN payment_method TYPE VARCHAR(50) USING payment_method::text;

ALTER TABLE orders
    ADD CONSTRAINT chk_order_status    CHECK (status         IN ('PLACED', 'CONFIRMED', 'SHIPPED', 'DELIVERED', 'RETURNED', 'CANCELLED')),
    ADD CONSTRAINT chk_payment_method  CHECK (payment_method IN ('STRIPE', 'PAYPAL', 'MBWAY'));

-- subscriptions
ALTER TABLE subscriptions
    ALTER COLUMN status TYPE VARCHAR(50) USING status::text;

ALTER TABLE subscriptions
    ADD CONSTRAINT chk_subscription_status CHECK (status IN ('ACTIVE', 'CANCELLED', 'EXPIRED'));

-- Drop the now-unused custom types (CASCADE handles any remaining dependents)
DROP TYPE user_role CASCADE;
DROP TYPE user_status CASCADE;
DROP TYPE shop_status CASCADE;
DROP TYPE product_status CASCADE;
DROP TYPE order_status CASCADE;
DROP TYPE payment_method CASCADE;
DROP TYPE subscription_status CASCADE;
