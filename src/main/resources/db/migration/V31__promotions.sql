-- Automatic, order-level promotions. Coupons remain code-based;
-- promotions are applied by the pricing engine when eligible.
CREATE TABLE promotion (
    id                   VARCHAR(50) PRIMARY KEY,
    created_at           TIMESTAMP    NOT NULL DEFAULT (NOW() AT TIME ZONE 'utc'),
    updated_at           TIMESTAMP,
    name                 VARCHAR(150) NOT NULL,
    type                 VARCHAR(30)  NOT NULL,
    value                DECIMAL(12, 2) NOT NULL,
    min_order_amount     DECIMAL(12, 2) NOT NULL DEFAULT 0,
    max_discount_amount  DECIMAL(12, 2),
    starts_at            TIMESTAMP    NOT NULL,
    ends_at              TIMESTAMP    NOT NULL,
    is_active            BOOLEAN      NOT NULL DEFAULT TRUE,
    priority             INTEGER      NOT NULL DEFAULT 0,
    usage_limit          INTEGER,
    usage_count          INTEGER      NOT NULL DEFAULT 0
);

CREATE INDEX promotion_active_window_idx ON promotion(is_active, starts_at, ends_at);
