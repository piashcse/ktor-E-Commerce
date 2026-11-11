-- Subscriptions and digital goods.
ALTER TABLE product ADD COLUMN IF NOT EXISTS is_digital BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE product ADD COLUMN IF NOT EXISTS digital_asset_url TEXT;
ALTER TABLE product ADD COLUMN IF NOT EXISTS digital_file_name VARCHAR(255);

ALTER TABLE order_item ADD COLUMN IF NOT EXISTS is_digital BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE IF NOT EXISTS subscription_plan (
    id          VARCHAR(50) PRIMARY KEY,
    created_at  TIMESTAMP    NOT NULL DEFAULT (NOW() AT TIME ZONE 'utc'),
    updated_at  TIMESTAMP,
    name        VARCHAR(150) NOT NULL,
    description TEXT         NOT NULL,
    product_id  VARCHAR(50)  NOT NULL REFERENCES product(id) ON DELETE CASCADE,
    interval    VARCHAR(20)  NOT NULL DEFAULT 'MONTH',
    price       DECIMAL(10, 2) NOT NULL,
    currency    VARCHAR(3)   NOT NULL DEFAULT 'USD',
    trial_days  INTEGER      NOT NULL DEFAULT 0,
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT subscription_plan_price_positive CHECK (price > 0)
);

CREATE INDEX IF NOT EXISTS subscription_plan_product_idx ON subscription_plan(product_id, is_active);

CREATE TABLE IF NOT EXISTS subscription (
    id                  VARCHAR(50) PRIMARY KEY,
    created_at          TIMESTAMP   NOT NULL DEFAULT (NOW() AT TIME ZONE 'utc'),
    updated_at          TIMESTAMP,
    user_id             VARCHAR(50) NOT NULL REFERENCES "user"(id) ON DELETE CASCADE,
    plan_id             VARCHAR(50) NOT NULL REFERENCES subscription_plan(id),
    status              VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    started_at          TIMESTAMP   NOT NULL DEFAULT (NOW() AT TIME ZONE 'utc'),
    current_period_start TIMESTAMP  NOT NULL DEFAULT (NOW() AT TIME ZONE 'utc'),
    current_period_end  TIMESTAMP   NOT NULL,
    cancel_at_period_end BOOLEAN    NOT NULL DEFAULT FALSE,
    external_reference  VARCHAR(150)
);

CREATE INDEX IF NOT EXISTS subscription_user_idx ON subscription(user_id);
CREATE INDEX IF NOT EXISTS subscription_status_idx ON subscription(status, current_period_end);

CREATE TABLE IF NOT EXISTS subscription_invoice (
    id                 VARCHAR(50) PRIMARY KEY,
    created_at         TIMESTAMP    NOT NULL DEFAULT (NOW() AT TIME ZONE 'utc'),
    updated_at         TIMESTAMP,
    subscription_id    VARCHAR(50)  NOT NULL REFERENCES subscription(id) ON DELETE CASCADE,
    amount             DECIMAL(10, 2) NOT NULL,
    currency           VARCHAR(3)   NOT NULL DEFAULT 'USD',
    status             VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    period_start       TIMESTAMP    NOT NULL,
    period_end         TIMESTAMP    NOT NULL,
    provider_reference VARCHAR(150),
    paid_at            TIMESTAMP
);

CREATE INDEX IF NOT EXISTS subscription_invoice_sub_idx ON subscription_invoice(subscription_id);

CREATE TABLE IF NOT EXISTS digital_entitlement (
    id              VARCHAR(50) PRIMARY KEY,
    created_at      TIMESTAMP    NOT NULL DEFAULT (NOW() AT TIME ZONE 'utc'),
    updated_at      TIMESTAMP,
    user_id         VARCHAR(50)  NOT NULL REFERENCES "user"(id) ON DELETE CASCADE,
    product_id      VARCHAR(50)  NOT NULL REFERENCES product(id) ON DELETE CASCADE,
    order_id        VARCHAR(50)  REFERENCES "order"(id) ON DELETE CASCADE,
    subscription_id VARCHAR(50)  REFERENCES subscription(id) ON DELETE CASCADE,
    download_token  VARCHAR(100) NOT NULL,
    download_url    TEXT         NOT NULL,
    file_name       VARCHAR(255),
    download_count  INTEGER      NOT NULL DEFAULT 0,
    max_downloads   INTEGER      NOT NULL DEFAULT 5,
    expires_at      TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS digital_entitlement_token_unique ON digital_entitlement(download_token);
CREATE INDEX IF NOT EXISTS digital_entitlement_user_idx ON digital_entitlement(user_id);
CREATE UNIQUE INDEX IF NOT EXISTS digital_entitlement_order_unique
  ON digital_entitlement(user_id, product_id, order_id)
  WHERE order_id IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS digital_entitlement_subscription_unique
  ON digital_entitlement(user_id, product_id, subscription_id)
  WHERE subscription_id IS NOT NULL;
