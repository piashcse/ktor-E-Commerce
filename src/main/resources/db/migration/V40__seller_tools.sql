-- Seller reviews, buyer/seller disputes and commission rules.
CREATE TABLE IF NOT EXISTS seller_review (
    id         VARCHAR(50) PRIMARY KEY,
    created_at TIMESTAMP   NOT NULL DEFAULT (NOW() AT TIME ZONE 'utc'),
    updated_at TIMESTAMP,
    seller_id  VARCHAR(50) NOT NULL REFERENCES seller(id) ON DELETE CASCADE,
    shop_id    VARCHAR(50) REFERENCES shop(id) ON DELETE SET NULL,
    user_id    VARCHAR(50) NOT NULL REFERENCES "user"(id) ON DELETE CASCADE,
    order_id   VARCHAR(50) NOT NULL REFERENCES "order"(id) ON DELETE CASCADE,
    rating     INTEGER     NOT NULL,
    comment    TEXT,
    status     VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    CONSTRAINT seller_review_rating_check CHECK (rating BETWEEN 1 AND 5)
);

CREATE UNIQUE INDEX IF NOT EXISTS seller_review_user_order_unique ON seller_review(user_id, order_id);
CREATE INDEX IF NOT EXISTS seller_review_seller_idx ON seller_review(seller_id, status);

CREATE TABLE IF NOT EXISTS seller_dispute (
    id          VARCHAR(50) PRIMARY KEY,
    created_at  TIMESTAMP    NOT NULL DEFAULT (NOW() AT TIME ZONE 'utc'),
    updated_at  TIMESTAMP,
    order_id    VARCHAR(50) REFERENCES "order"(id) ON DELETE SET NULL,
    seller_id   VARCHAR(50) REFERENCES seller(id) ON DELETE SET NULL,
    user_id     VARCHAR(50) NOT NULL REFERENCES "user"(id) ON DELETE CASCADE,
    type        VARCHAR(30)  NOT NULL,
    subject     VARCHAR(200) NOT NULL,
    description TEXT         NOT NULL,
    status      VARCHAR(30)  NOT NULL DEFAULT 'OPEN',
    resolution  TEXT,
    resolved_by VARCHAR(50)  REFERENCES "user"(id) ON DELETE SET NULL,
    resolved_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS seller_dispute_user_idx ON seller_dispute(user_id, created_at);
CREATE INDEX IF NOT EXISTS seller_dispute_seller_idx ON seller_dispute(seller_id, status);
CREATE INDEX IF NOT EXISTS seller_dispute_status_idx ON seller_dispute(status);

CREATE TABLE IF NOT EXISTS commission_rule (
    id          VARCHAR(50) PRIMARY KEY,
    created_at  TIMESTAMP    NOT NULL DEFAULT (NOW() AT TIME ZONE 'utc'),
    updated_at  TIMESTAMP,
    name        VARCHAR(150) NOT NULL,
    seller_id   VARCHAR(50)  REFERENCES seller(id) ON DELETE CASCADE,
    shop_id     VARCHAR(50)  REFERENCES shop(id) ON DELETE CASCADE,
    category_id VARCHAR(50)  REFERENCES category(id) ON DELETE CASCADE,
    rate        DECIMAL(5, 2) NOT NULL,
    priority    INTEGER      NOT NULL DEFAULT 0,
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT commission_rule_rate_check CHECK (rate >= 0 AND rate <= 100)
);

CREATE INDEX IF NOT EXISTS commission_rule_active_idx ON commission_rule(is_active, priority);
CREATE INDEX IF NOT EXISTS commission_rule_seller_idx ON commission_rule(seller_id);
