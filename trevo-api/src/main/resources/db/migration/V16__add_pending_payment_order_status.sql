-- ============================================================
-- V16: Add payment-pending order status
-- ============================================================

ALTER TABLE orders
    DROP CONSTRAINT IF EXISTS chk_order_status;

ALTER TABLE orders
    ADD CONSTRAINT chk_order_status
        CHECK (status IN ('PENDING_PAYMENT', 'PLACED', 'CONFIRMED', 'SHIPPED', 'DELIVERED', 'RETURNED', 'CANCELLED'));
