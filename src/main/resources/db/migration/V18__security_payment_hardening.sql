-- V18: security + payment hardening (PR 1).
-- Additive only. Applied migrations are immutable.

-- ── OTP attempts: drop the pre-V12 unique index that blocks RESET rows ────────
DROP INDEX IF EXISTS otp_attempt_user_id_idx;
CREATE UNIQUE INDEX IF NOT EXISTS otp_attempt_user_purpose_unique ON otp_attempt(user_id, purpose);

-- ── Payments: currency + per-user idempotency key (separate from transaction_id) ─
ALTER TABLE payment ADD COLUMN IF NOT EXISTS currency VARCHAR(3) NOT NULL DEFAULT 'USD';
ALTER TABLE payment ADD COLUMN IF NOT EXISTS idempotency_key VARCHAR(100);
CREATE UNIQUE INDEX IF NOT EXISTS payment_user_idempotency_unique
  ON payment(user_id, idempotency_key)
  WHERE idempotency_key IS NOT NULL;
CREATE INDEX IF NOT EXISTS payment_user_id_idx ON payment(user_id);

-- ── Seller status drift: DB default was the invalid enum value 'ACTIVE' ──────
ALTER TABLE seller ALTER COLUMN status SET DEFAULT 'PENDING';
UPDATE seller SET status = 'PENDING'
  WHERE status NOT IN ('PENDING', 'APPROVED', 'REJECTED', 'SUSPENDED');
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'seller_status_check') THEN
        ALTER TABLE seller ADD CONSTRAINT seller_status_check
          CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'SUSPENDED'));
    END IF;
END $$;

-- ── Missing auth hot-path index ─────────────────────────────────────────────
CREATE INDEX IF NOT EXISTS refresh_token_user_id_idx ON refresh_token(user_id);

-- Shared entity column used by the refund mapper (additive; commerce PR fills it).
ALTER TABLE refund_request ADD COLUMN IF NOT EXISTS currency VARCHAR(3) NOT NULL DEFAULT 'USD';
