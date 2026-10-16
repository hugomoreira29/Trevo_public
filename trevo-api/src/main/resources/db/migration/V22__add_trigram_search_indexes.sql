-- NFR-03: Enable pg_trgm so that lower(col) LIKE '%keyword%' queries can use GIN indexes.
-- The existing GIN indexes (to_tsvector) are for full-text search and are NOT used by LIKE.
--
-- NOTE: On managed databases (AWS RDS, Azure Database for PostgreSQL) pg_trgm is available
-- but requires superuser or the provider's elevated role (e.g. rds_superuser on RDS).
-- Verify the extension can be created in staging/production before deploying this migration.
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- Expression-based GIN trigram indexes matching the lower(col) LIKE pattern in
-- ProductSpecification.keywordMatches
CREATE INDEX idx_products_name_trgm   ON products USING GIN (lower(name)        gin_trgm_ops);
CREATE INDEX idx_products_desc_trgm   ON products USING GIN (lower(description)  gin_trgm_ops);

-- B-tree index for price range filter queries
CREATE INDEX idx_products_price       ON products (price);

-- Composite index for the common (status, shop_id) filter combination
CREATE INDEX idx_products_status_shop ON products (status, shop_id);
