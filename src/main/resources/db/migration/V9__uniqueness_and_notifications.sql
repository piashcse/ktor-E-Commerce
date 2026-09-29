-- V9: uniqueness guards (wishlist/cart/review), coupon_usage dedup, notification table, order idempotency partial index.

-- 1) Deduplicate existing rows before adding unique constraints (keep earliest).
DELETE FROM wishlist a USING wishlist b
WHERE a.id > b.id AND a.user_id = b.user_id AND a.product_id = b.product_id;

DELETE FROM cart_item a USING cart_item b
WHERE a.id > b.id AND a.user_id = b.user_id AND a.product_id = b.product_id;

DELETE FROM review_rating a USING review_rating b
WHERE a.id > b.id AND a.user_id = b.user_id AND a.product_id = b.product_id;

DELETE FROM coupon_usage a USING coupon_usage b
WHERE a.id > b.id AND a.coupon_id = b.coupon_id AND a.user_id = b.user_id AND COALESCE(a.order_id, '') = COALESCE(b.order_id, '');

-- 2) Unique constraints (app check-then-insert races become DB-guarded 409s).
DO $$ BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'wishlist_user_product_unique') THEN
    ALTER TABLE wishlist ADD CONSTRAINT wishlist_user_product_unique UNIQUE (user_id, product_id);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'cart_item_user_product_unique') THEN
    ALTER TABLE cart_item ADD CONSTRAINT cart_item_user_product_unique UNIQUE (user_id, product_id);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'review_rating_user_product_unique') THEN
    ALTER TABLE review_rating ADD CONSTRAINT review_rating_user_product_unique UNIQUE (user_id, product_id);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'coupon_usage_coupon_user_order_unique') THEN
    ALTER TABLE coupon_usage ADD CONSTRAINT coupon_usage_coupon_user_order_unique UNIQUE (coupon_id, user_id, order_id);
  END IF;
END $$;

-- 3) Order idempotency: NULL keys stay distinct in PG; enforce uniqueness only for non-NULL keys.
DROP INDEX IF EXISTS order_idempotency_key;
CREATE UNIQUE INDEX IF NOT EXISTS order_idempotency_key_not_null
  ON "order"(idempotency_key) WHERE idempotency_key IS NOT NULL;

-- 4) Missing hot-path indexes.
CREATE INDEX IF NOT EXISTS payment_order_id_idx ON payment(order_id);
CREATE INDEX IF NOT EXISTS order_item_order_id_idx ON order_item(order_id);
CREATE INDEX IF NOT EXISTS refund_request_order_id_idx ON refund_request(order_id);
CREATE INDEX IF NOT EXISTS refund_request_order_item_id_idx ON refund_request(order_item_id);

-- 5) Notification table (email-only today; enables push/SMS/in-app + prefs).
CREATE TABLE IF NOT EXISTS notification (
  id VARCHAR(50) PRIMARY KEY,
  created_at TIMESTAMP NOT NULL DEFAULT (NOW() AT TIME ZONE 'utc'),
  updated_at TIMESTAMP,
  user_id VARCHAR(50) NOT NULL REFERENCES "user"(id) ON DELETE CASCADE,
  channel VARCHAR(20) NOT NULL DEFAULT 'EMAIL',
  type VARCHAR(50) NOT NULL,
  title VARCHAR(255) NOT NULL,
  body TEXT,
  resource_type VARCHAR(50),
  resource_id VARCHAR(50),
  is_read BOOLEAN NOT NULL DEFAULT FALSE
);
CREATE INDEX IF NOT EXISTS notification_user_id_idx ON notification(user_id);
CREATE INDEX IF NOT EXISTS notification_user_read_idx ON notification(user_id, is_read);
