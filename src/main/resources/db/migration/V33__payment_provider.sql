-- Provider-backed payments: Stripe-style intents, SCA/3DS state and signed webhooks.
ALTER TABLE payment ADD COLUMN IF NOT EXISTS provider VARCHAR(30) NOT NULL DEFAULT 'MANUAL';
ALTER TABLE payment ADD COLUMN IF NOT EXISTS provider_reference VARCHAR(150);
ALTER TABLE payment ADD COLUMN IF NOT EXISTS client_secret VARCHAR(255);
ALTER TABLE payment ADD COLUMN IF NOT EXISTS next_action_url VARCHAR(1000);
ALTER TABLE payment ADD COLUMN IF NOT EXISTS sca_status VARCHAR(30) NOT NULL DEFAULT 'NOT_REQUIRED';
ALTER TABLE payment ADD COLUMN IF NOT EXISTS provider_status VARCHAR(50);

CREATE UNIQUE INDEX IF NOT EXISTS payment_provider_reference_unique
  ON payment(provider_reference) WHERE provider_reference IS NOT NULL;
CREATE INDEX IF NOT EXISTS payment_provider_reference_idx
  ON payment(provider_reference);

-- At-least-once provider deliveries are deduplicated by (provider, event_id).
CREATE TABLE IF NOT EXISTS payment_webhook_event (
    id           VARCHAR(50) PRIMARY KEY,
    created_at   TIMESTAMP    NOT NULL DEFAULT (NOW() AT TIME ZONE 'utc'),
    updated_at   TIMESTAMP,
    provider     VARCHAR(30)  NOT NULL,
    event_id     VARCHAR(150) NOT NULL,
    event_type   VARCHAR(100) NOT NULL,
    status       VARCHAR(20)  NOT NULL DEFAULT 'RECEIVED',
    payload      TEXT         NOT NULL,
    error        TEXT,
    received_at  TIMESTAMP    NOT NULL DEFAULT (NOW() AT TIME ZONE 'utc'),
    processed_at TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS payment_webhook_event_provider_event_unique
  ON payment_webhook_event(provider, event_id);

-- Provider-side refund tracking. The local refund ledger remains the source of truth
-- for stock/payout effects; these columns mirror the processor state.
ALTER TABLE refund_request ADD COLUMN IF NOT EXISTS provider_refund_id VARCHAR(150);
ALTER TABLE refund_request ADD COLUMN IF NOT EXISTS provider_refund_status VARCHAR(30);
CREATE INDEX IF NOT EXISTS refund_request_provider_refund_idx
  ON refund_request(provider_refund_id);
