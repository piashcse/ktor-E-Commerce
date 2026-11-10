-- Fraud/risk controls, AVS/CVV storage and chargeback records.
CREATE TABLE IF NOT EXISTS fraud_rule (
    id             VARCHAR(50) PRIMARY KEY,
    created_at     TIMESTAMP     NOT NULL DEFAULT (NOW() AT TIME ZONE 'utc'),
    updated_at     TIMESTAMP,
    name           VARCHAR(150)  NOT NULL,
    metric         VARCHAR(30)   NOT NULL,
    window_minutes INTEGER       NOT NULL DEFAULT 60,
    threshold      DECIMAL(12, 2) NOT NULL,
    action         VARCHAR(20)   NOT NULL DEFAULT 'REVIEW',
    priority       INTEGER       NOT NULL DEFAULT 0,
    is_active      BOOLEAN       NOT NULL DEFAULT TRUE,
    CONSTRAINT fraud_rule_threshold_positive CHECK (threshold >= 0),
    CONSTRAINT fraud_rule_window_positive CHECK (window_minutes > 0)
);

CREATE INDEX IF NOT EXISTS fraud_rule_active_idx ON fraud_rule(is_active, priority);

CREATE TABLE IF NOT EXISTS blocklist_entry (
    id          VARCHAR(50) PRIMARY KEY,
    created_at  TIMESTAMP    NOT NULL DEFAULT (NOW() AT TIME ZONE 'utc'),
    updated_at  TIMESTAMP,
    type        VARCHAR(20)  NOT NULL,
    value       VARCHAR(255) NOT NULL,
    reason      TEXT,
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
    expires_at  TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS blocklist_entry_type_value_unique
  ON blocklist_entry(type, value);

CREATE TABLE IF NOT EXISTS payment_risk (
    id             VARCHAR(50) PRIMARY KEY,
    created_at     TIMESTAMP    NOT NULL DEFAULT (NOW() AT TIME ZONE 'utc'),
    updated_at     TIMESTAMP,
    payment_id     VARCHAR(50)  NOT NULL REFERENCES payment(id) ON DELETE CASCADE,
    score          INTEGER      NOT NULL DEFAULT 0,
    decision       VARCHAR(20)  NOT NULL DEFAULT 'ALLOW',
    reasons        TEXT,
    avs_status     VARCHAR(20)  NOT NULL DEFAULT 'UNKNOWN',
    cvv_status     VARCHAR(20)  NOT NULL DEFAULT 'UNKNOWN',
    requires_3ds   BOOLEAN      NOT NULL DEFAULT FALSE,
    evaluated_at   TIMESTAMP    NOT NULL DEFAULT (NOW() AT TIME ZONE 'utc')
);

CREATE INDEX IF NOT EXISTS payment_risk_payment_idx ON payment_risk(payment_id);
CREATE INDEX IF NOT EXISTS payment_risk_decision_idx ON payment_risk(decision);

ALTER TABLE payment ADD COLUMN IF NOT EXISTS risk_score INTEGER NOT NULL DEFAULT 0;
ALTER TABLE payment ADD COLUMN IF NOT EXISTS risk_decision VARCHAR(20) NOT NULL DEFAULT 'ALLOW';
ALTER TABLE payment ADD COLUMN IF NOT EXISTS avs_status VARCHAR(20) NOT NULL DEFAULT 'UNKNOWN';
ALTER TABLE payment ADD COLUMN IF NOT EXISTS cvv_status VARCHAR(20) NOT NULL DEFAULT 'UNKNOWN';
ALTER TABLE payment ADD COLUMN IF NOT EXISTS three_ds_enforced BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE IF NOT EXISTS chargeback (
    id                  VARCHAR(50) PRIMARY KEY,
    created_at          TIMESTAMP     NOT NULL DEFAULT (NOW() AT TIME ZONE 'utc'),
    updated_at          TIMESTAMP,
    payment_id          VARCHAR(50)   NOT NULL REFERENCES payment(id),
    order_id            VARCHAR(50)   NOT NULL REFERENCES "order"(id),
    provider_dispute_id VARCHAR(150),
    amount              DECIMAL(10, 2) NOT NULL,
    currency            VARCHAR(3)    NOT NULL DEFAULT 'USD',
    status              VARCHAR(30)   NOT NULL DEFAULT 'OPEN',
    reason              TEXT,
    outcome             TEXT,
    evidence_due_at     TIMESTAMP,
    opened_at           TIMESTAMP     NOT NULL DEFAULT (NOW() AT TIME ZONE 'utc'),
    resolved_at         TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS chargeback_provider_dispute_unique
  ON chargeback(provider_dispute_id) WHERE provider_dispute_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS chargeback_payment_idx ON chargeback(payment_id);
CREATE INDEX IF NOT EXISTS chargeback_order_idx ON chargeback(order_id);
CREATE INDEX IF NOT EXISTS chargeback_status_idx ON chargeback(status);
