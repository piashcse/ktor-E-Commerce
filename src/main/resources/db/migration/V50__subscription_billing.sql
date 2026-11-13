-- Recurring subscription billing, dunning and webhook activation.

ALTER TABLE subscription ADD COLUMN IF NOT EXISTS payment_method_reference VARCHAR(255);
ALTER TABLE subscription ADD COLUMN IF NOT EXISTS provider_customer_reference VARCHAR(255);
ALTER TABLE subscription ADD COLUMN IF NOT EXISTS dunning_attempts INTEGER NOT NULL DEFAULT 0;
ALTER TABLE subscription ADD COLUMN IF NOT EXISTS grace_until TIMESTAMP;

ALTER TABLE subscription_invoice ADD COLUMN IF NOT EXISTS attempt_count INTEGER NOT NULL DEFAULT 0;
ALTER TABLE subscription_invoice ADD COLUMN IF NOT EXISTS last_attempt_at TIMESTAMP;
ALTER TABLE subscription_invoice ADD COLUMN IF NOT EXISTS next_retry_at TIMESTAMP;
ALTER TABLE subscription_invoice ADD COLUMN IF NOT EXISTS failure_reason TEXT;

-- A subscription may have at most one invoice per billing period. This makes the
-- billing run idempotent under concurrent schedulers.
CREATE UNIQUE INDEX IF NOT EXISTS subscription_invoice_period_unique
  ON subscription_invoice(subscription_id, period_start);

CREATE INDEX IF NOT EXISTS subscription_invoice_retry_idx
  ON subscription_invoice(status, next_retry_at);
