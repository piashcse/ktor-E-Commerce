-- V19: commerce, refunds, payouts, loyalty and policy integrity (PR 2).
-- Additive only. Applied migrations are immutable.

-- ── Orders: persist the FX rate applied to the order and scope idempotency keys ─
ALTER TABLE "order" ADD COLUMN IF NOT EXISTS fx_rate DECIMAL(18,6) NOT NULL DEFAULT 1;
ALTER TABLE "order" ALTER COLUMN total TYPE DECIMAL(12,2);
ALTER TABLE "order" ALTER COLUMN sub_total TYPE DECIMAL(12,2);
ALTER TABLE "order" ALTER COLUMN shipping_cost TYPE DECIMAL(12,2);
ALTER TABLE "order" ALTER COLUMN tax_amount TYPE DECIMAL(12,2);
ALTER TABLE "order" ALTER COLUMN discount_amount TYPE DECIMAL(12,2);

DROP INDEX IF EXISTS order_idempotency_key_idx;
DROP INDEX IF EXISTS order_idempotency_key_not_null;
CREATE UNIQUE INDEX IF NOT EXISTS order_user_idempotency_unique
  ON "order"(user_id, idempotency_key)
  WHERE idempotency_key IS NOT NULL;

-- ── Coupons: per-user limit + cascade usage cleanup ──────────────────────────
ALTER TABLE coupon ADD COLUMN IF NOT EXISTS per_user_limit INTEGER NOT NULL DEFAULT 1;
ALTER TABLE coupon DROP CONSTRAINT IF EXISTS coupon_per_user_limit_check;
ALTER TABLE coupon ADD CONSTRAINT coupon_per_user_limit_check CHECK (per_user_limit >= 1);

DO $$
DECLARE
    conname TEXT;
BEGIN
    SELECT c.conname INTO conname
    FROM pg_constraint c
    JOIN pg_attribute a ON a.attnum = ANY(c.conkey) AND a.attrelid = c.conrelid
    WHERE c.conrelid = 'coupon_usage'::regclass
      AND c.confrelid = 'coupon'::regclass
      AND c.contype = 'f'
      AND a.attname = 'coupon_id';
    IF conname IS NOT NULL THEN
        EXECUTE format('ALTER TABLE coupon_usage DROP CONSTRAINT %I', conname);
    END IF;
END $$;
ALTER TABLE coupon_usage
  ADD CONSTRAINT coupon_usage_coupon_id_fkey
  FOREIGN KEY (coupon_id) REFERENCES coupon(id) ON DELETE CASCADE;
CREATE INDEX IF NOT EXISTS coupon_usage_order_id_idx ON coupon_usage(order_id);

-- ── Refunds: currency used by the order ──────────────────────────────────────
ALTER TABLE refund_request ADD COLUMN IF NOT EXISTS currency VARCHAR(3) NOT NULL DEFAULT 'USD';
CREATE INDEX IF NOT EXISTS refund_request_user_id_idx ON refund_request(user_id);

-- ── Payouts: per-seller request aggregate, currency, lifecycle statuses ──────
ALTER TABLE seller_payout ADD COLUMN IF NOT EXISTS currency VARCHAR(3) NOT NULL DEFAULT 'USD';
ALTER TABLE seller_payout ADD COLUMN IF NOT EXISTS payout_request_id VARCHAR(50);
DROP INDEX IF EXISTS seller_payout_key_not_null;
CREATE INDEX IF NOT EXISTS seller_payout_payout_request_idx ON seller_payout(payout_request_id);
CREATE INDEX IF NOT EXISTS seller_payout_order_id_idx ON seller_payout(order_id);

CREATE TABLE IF NOT EXISTS payout_request (
    id              VARCHAR(50) PRIMARY KEY,
    created_at      TIMESTAMP NOT NULL DEFAULT (NOW() AT TIME ZONE 'utc'),
    updated_at      TIMESTAMP,
    seller_id       VARCHAR(50) NOT NULL REFERENCES seller(id) ON DELETE CASCADE,
    idempotency_key VARCHAR(100),
    amount          DECIMAL(12,2) NOT NULL,
    currency        VARCHAR(3) NOT NULL DEFAULT 'USD',
    status          VARCHAR(20) NOT NULL DEFAULT 'REQUESTED'
);
CREATE INDEX IF NOT EXISTS payout_request_seller_idx ON payout_request(seller_id);
CREATE INDEX IF NOT EXISTS payout_request_status_idx ON payout_request(status);
CREATE UNIQUE INDEX IF NOT EXISTS payout_request_seller_key_unique
  ON payout_request(seller_id, idempotency_key)
  WHERE idempotency_key IS NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'seller_payout_payout_request_fk') THEN
        ALTER TABLE seller_payout
          ADD CONSTRAINT seller_payout_payout_request_fk
          FOREIGN KEY (payout_request_id) REFERENCES payout_request(id) ON DELETE SET NULL;
    END IF;
END $$;

-- Backfill lifecycle statuses inferred from timestamps.
UPDATE seller_payout SET status = 'REQUESTED'
  WHERE requested_at IS NOT NULL AND status = 'PENDING' AND approved_at IS NULL AND rejected_at IS NULL;
UPDATE seller_payout SET status = 'APPROVED'
  WHERE approved_at IS NOT NULL AND status = 'PENDING' AND paid_at IS NULL AND rejected_at IS NULL;
UPDATE seller_payout SET status = 'REJECTED'
  WHERE rejected_at IS NOT NULL AND status = 'PENDING';

-- ── Consent history: revocation instead of hard delete ──────────────────────
ALTER TABLE policy_consents ADD COLUMN IF NOT EXISTS revoked_at TIMESTAMP;
CREATE INDEX IF NOT EXISTS policy_consents_user_id_idx ON policy_consents(user_id);
CREATE INDEX IF NOT EXISTS policy_consents_policy_id_idx ON policy_consents(policy_id);

-- ── Review helpful votes: one vote per user per review ──────────────────────
CREATE TABLE IF NOT EXISTS review_helpful_vote (
    id          VARCHAR(50) PRIMARY KEY,
    created_at  TIMESTAMP NOT NULL DEFAULT (NOW() AT TIME ZONE 'utc'),
    updated_at  TIMESTAMP,
    review_id   VARCHAR(50) NOT NULL REFERENCES review_rating(id) ON DELETE CASCADE,
    user_id     VARCHAR(50) NOT NULL REFERENCES "user"(id) ON DELETE CASCADE,
    helpful     BOOLEAN NOT NULL
);
CREATE UNIQUE INDEX IF NOT EXISTS review_helpful_vote_unique ON review_helpful_vote(review_id, user_id);

-- ── Missing commerce indexes ────────────────────────────────────────────────
CREATE INDEX IF NOT EXISTS order_status_history_order_id_idx ON order_status_history(order_id);
CREATE INDEX IF NOT EXISTS order_item_product_id_idx ON order_item(product_id);
CREATE INDEX IF NOT EXISTS order_item_shop_id_idx ON order_item(shop_id);

-- ── One active policy per type (partial unique index) ───────────────────────
-- Keep the newest active policy per type before enforcing uniqueness.
UPDATE policy_documents p SET is_active = FALSE
WHERE p.is_active = TRUE
  AND EXISTS (
      SELECT 1 FROM policy_documents q
      WHERE q.type = p.type
        AND q.is_active = TRUE
        AND (q.effective_date, q.id) > (p.effective_date, p.id)
  );
CREATE UNIQUE INDEX IF NOT EXISTS policy_documents_one_active_per_type
  ON policy_documents(type)
  WHERE is_active = TRUE;
