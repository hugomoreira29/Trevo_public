-- ============================================================
-- V16: In-app notifications
-- ============================================================

CREATE TABLE notifications (
    id              BIGSERIAL       PRIMARY KEY,
    recipient_id    BIGINT          NOT NULL,
    order_id        BIGINT,
    type            VARCHAR(50)     NOT NULL,
    title           VARCHAR(160)    NOT NULL,
    message         VARCHAR(500)    NOT NULL,
    read_at         TIMESTAMP,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_notifications_recipient
        FOREIGN KEY (recipient_id) REFERENCES users (id) ON DELETE CASCADE,

    CONSTRAINT fk_notifications_order
        FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE,

    CONSTRAINT chk_notification_type
        CHECK (type IN ('NEW_ORDER'))
);

CREATE INDEX idx_notifications_recipient_created
    ON notifications (recipient_id, created_at DESC);

CREATE INDEX idx_notifications_recipient_unread
    ON notifications (recipient_id)
    WHERE read_at IS NULL;
