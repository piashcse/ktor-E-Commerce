-- V7: Fix schema/entity drift between migrations and Exposed entities
-- 1) order_status_history.status INTEGER -> VARCHAR(30) (matches enumerationByName)
-- 2) inventory.status INTEGER -> VARCHAR(50) (matches enumerationByName)
-- 3) payment.amount BIGINT -> DECIMAL(10,2) (cents-precision money)
-- 4) coupon money columns DOUBLE PRECISION -> DECIMAL(10,2)
-- 5) shipping_method.price DOUBLE PRECISION -> DECIMAL(10,2)
-- 6) partial unique index preventing double COMPLETED payment per order

ALTER TABLE order_status_history ALTER COLUMN status TYPE VARCHAR(30)
    USING CASE status
        WHEN 0 THEN 'PENDING'
        WHEN 1 THEN 'CONFIRMED'
        WHEN 2 THEN 'PAID'
        WHEN 3 THEN 'DELIVERED'
        WHEN 4 THEN 'CANCELED'
        ELSE 'RECEIVED'
    END;
ALTER TABLE order_status_history ALTER COLUMN status SET NOT NULL;

ALTER TABLE inventory ALTER COLUMN status TYPE VARCHAR(50)
    USING CASE status
        WHEN 0 THEN 'IN_STOCK'
        WHEN 1 THEN 'LOW_STOCK'
        ELSE 'OUT_OF_STOCK'
    END;
ALTER TABLE inventory ALTER COLUMN status SET DEFAULT 'IN_STOCK';
ALTER TABLE inventory ALTER COLUMN status SET NOT NULL;

ALTER TABLE payment ALTER COLUMN amount TYPE DECIMAL(10,2);

ALTER TABLE coupon ALTER COLUMN discount_value TYPE DECIMAL(10,2);
ALTER TABLE coupon ALTER COLUMN min_order_amount TYPE DECIMAL(10,2);
ALTER TABLE coupon ALTER COLUMN max_discount_amount TYPE DECIMAL(10,2);

ALTER TABLE shipping_method ALTER COLUMN price TYPE DECIMAL(10,2);

CREATE UNIQUE INDEX IF NOT EXISTS payment_order_completed_idx ON payment(order_id) WHERE status = 'COMPLETED';
