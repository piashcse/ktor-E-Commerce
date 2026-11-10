-- Loyalty points, referrals, gift cards and store credit.
CREATE TABLE IF NOT EXISTS wallet_account (
    id              VARCHAR(50) PRIMARY KEY,
    created_at      TIMESTAMP     NOT NULL DEFAULT (NOW() AT TIME ZONE 'utc'),
    updated_at      TIMESTAMP,
    user_id         VARCHAR(50)   NOT NULL REFERENCES "user"(id) ON DELETE CASCADE,
    credit_balance  DECIMAL(12, 2) NOT NULL DEFAULT 0,
    points_balance  INTEGER       NOT NULL DEFAULT 0,
    lifetime_points INTEGER       NOT NULL DEFAULT 0,
    referral_code   VARCHAR(20)   NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS wallet_account_user_unique ON wallet_account(user_id);
CREATE UNIQUE INDEX IF NOT EXISTS wallet_account_referral_code_unique ON wallet_account(referral_code);

CREATE TABLE IF NOT EXISTS wallet_ledger (
    id             VARCHAR(50) PRIMARY KEY,
    created_at     TIMESTAMP     NOT NULL DEFAULT (NOW() AT TIME ZONE 'utc'),
    updated_at     TIMESTAMP,
    user_id        VARCHAR(50)   NOT NULL REFERENCES "user"(id) ON DELETE CASCADE,
    type           VARCHAR(30)   NOT NULL,
    points_delta   INTEGER       NOT NULL DEFAULT 0,
    credit_delta   DECIMAL(12, 2) NOT NULL DEFAULT 0,
    reference_type VARCHAR(50),
    reference_id   VARCHAR(100),
    note           TEXT
);

CREATE INDEX IF NOT EXISTS wallet_ledger_user_idx ON wallet_ledger(user_id);
CREATE UNIQUE INDEX IF NOT EXISTS wallet_ledger_idempotency
  ON wallet_ledger(user_id, type, reference_type, reference_id)
  WHERE reference_id IS NOT NULL;

CREATE TABLE IF NOT EXISTS referral (
    id               VARCHAR(50) PRIMARY KEY,
    created_at       TIMESTAMP    NOT NULL DEFAULT (NOW() AT TIME ZONE 'utc'),
    updated_at       TIMESTAMP,
    referrer_user_id VARCHAR(50)  NOT NULL REFERENCES "user"(id) ON DELETE CASCADE,
    referee_user_id  VARCHAR(50)  NOT NULL REFERENCES "user"(id) ON DELETE CASCADE,
    code             VARCHAR(20)  NOT NULL,
    status           VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    reward_points    INTEGER      NOT NULL DEFAULT 0,
    reward_credit    DECIMAL(12, 2) NOT NULL DEFAULT 0,
    completed_at     TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS referral_referee_unique ON referral(referee_user_id);
CREATE INDEX IF NOT EXISTS referral_referrer_idx ON referral(referrer_user_id);

CREATE TABLE IF NOT EXISTS gift_card (
    id              VARCHAR(50) PRIMARY KEY,
    created_at      TIMESTAMP     NOT NULL DEFAULT (NOW() AT TIME ZONE 'utc'),
    updated_at      TIMESTAMP,
    code            VARCHAR(50)   NOT NULL,
    initial_balance DECIMAL(12, 2) NOT NULL,
    balance         DECIMAL(12, 2) NOT NULL,
    currency        VARCHAR(3)    NOT NULL DEFAULT 'USD',
    status          VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',
    issued_by       VARCHAR(50)   REFERENCES "user"(id),
    expires_at      TIMESTAMP,
    CONSTRAINT gift_card_balance_non_negative CHECK (balance >= 0)
);

CREATE UNIQUE INDEX IF NOT EXISTS gift_card_code_unique ON gift_card(code);
CREATE INDEX IF NOT EXISTS gift_card_status_idx ON gift_card(status);

CREATE TABLE IF NOT EXISTS gift_card_redemption (
    id           VARCHAR(50) PRIMARY KEY,
    created_at   TIMESTAMP     NOT NULL DEFAULT (NOW() AT TIME ZONE 'utc'),
    updated_at   TIMESTAMP,
    gift_card_id VARCHAR(50)   NOT NULL REFERENCES gift_card(id),
    order_id     VARCHAR(50)   NOT NULL REFERENCES "order"(id),
    user_id      VARCHAR(50)   NOT NULL REFERENCES "user"(id),
    amount       DECIMAL(12, 2) NOT NULL
);

CREATE INDEX IF NOT EXISTS gift_card_redemption_card_idx ON gift_card_redemption(gift_card_id);
CREATE INDEX IF NOT EXISTS gift_card_redemption_order_idx ON gift_card_redemption(order_id);

ALTER TABLE "order" ADD COLUMN IF NOT EXISTS wallet_discount DECIMAL(10, 2) NOT NULL DEFAULT 0;
ALTER TABLE "order" ADD COLUMN IF NOT EXISTS gift_card_discount DECIMAL(10, 2) NOT NULL DEFAULT 0;
ALTER TABLE "order" ADD COLUMN IF NOT EXISTS gift_card_code VARCHAR(50);
