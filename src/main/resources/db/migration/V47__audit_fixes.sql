-- Audit/hardening fixes.
ALTER TABLE shipment ADD COLUMN IF NOT EXISTS idempotency_key VARCHAR(100);
CREATE UNIQUE INDEX IF NOT EXISTS shipment_order_idempotency_unique
  ON shipment(order_id, idempotency_key) WHERE idempotency_key IS NOT NULL;

ALTER TABLE seller_payout ADD COLUMN IF NOT EXISTS commission_rate DECIMAL(5, 2);

ALTER TABLE "user" ADD COLUMN IF NOT EXISTS mfa_last_used_step BIGINT;
ALTER TABLE "user" ALTER COLUMN mfa_secret TYPE VARCHAR(255);

CREATE INDEX IF NOT EXISTS product_shop_status_idx ON product(shop_id, status);
