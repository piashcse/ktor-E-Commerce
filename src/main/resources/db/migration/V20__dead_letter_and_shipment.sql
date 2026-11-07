-- V20: dead-letter visibility and shipment/refund completeness.
-- Additive only. Applied migrations are immutable.

CREATE TABLE IF NOT EXISTS outbox_dead_letter (
    id             VARCHAR(50) PRIMARY KEY,
    created_at     TIMESTAMP NOT NULL DEFAULT (NOW() AT TIME ZONE 'utc'),
    updated_at     TIMESTAMP,
    event_type     VARCHAR(50) NOT NULL,
    aggregate_type VARCHAR(50) NOT NULL,
    aggregate_id   VARCHAR(50) NOT NULL,
    payload        TEXT NOT NULL,
    error          TEXT,
    attempts       INTEGER NOT NULL
);
CREATE INDEX IF NOT EXISTS outbox_dead_letter_event_type_idx ON outbox_dead_letter(event_type);

ALTER TABLE "order" ADD COLUMN IF NOT EXISTS tracking_number VARCHAR(100);

ALTER TABLE refund_request ADD COLUMN IF NOT EXISTS quantity INTEGER NOT NULL DEFAULT 1;
ALTER TABLE refund_request ADD COLUMN IF NOT EXISTS shipping_refund DECIMAL(10,2) NOT NULL DEFAULT 0.00;
ALTER TABLE refund_request ADD COLUMN IF NOT EXISTS tax_refund DECIMAL(10,2) NOT NULL DEFAULT 0.00;

-- Optional coupon scoping: NULL means the coupon applies to the whole catalog.
ALTER TABLE coupon ADD COLUMN IF NOT EXISTS shop_id VARCHAR(50) REFERENCES shop(id) ON DELETE SET NULL;
ALTER TABLE coupon ADD COLUMN IF NOT EXISTS category_id VARCHAR(50) REFERENCES category(id) ON DELETE SET NULL;
CREATE INDEX IF NOT EXISTS coupon_shop_id_idx ON coupon(shop_id);
CREATE INDEX IF NOT EXISTS coupon_category_id_idx ON coupon(category_id);
