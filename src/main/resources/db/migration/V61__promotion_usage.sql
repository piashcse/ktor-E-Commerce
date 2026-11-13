CREATE TABLE IF NOT EXISTS promotion_usage (
    id                  VARCHAR(50) PRIMARY KEY,
    created_at          TIMESTAMP   NOT NULL DEFAULT (NOW() AT TIME ZONE 'utc'),
    updated_at          TIMESTAMP,
    promotion_id        VARCHAR(50) NOT NULL REFERENCES promotion(id) ON DELETE CASCADE,
    user_id             VARCHAR(50) NOT NULL REFERENCES "user"(id) ON DELETE CASCADE,
    order_id            VARCHAR(50) NOT NULL REFERENCES "order"(id) ON DELETE CASCADE,
    checkout_reference  VARCHAR(64) NOT NULL,
    used_at             TIMESTAMP   NOT NULL DEFAULT (NOW() AT TIME ZONE 'utc'),
    reversed_at         TIMESTAMP
);

CREATE INDEX IF NOT EXISTS promotion_usage_promotion_id_idx ON promotion_usage (promotion_id);
CREATE INDEX IF NOT EXISTS promotion_usage_user_id_idx ON promotion_usage (user_id);
CREATE INDEX IF NOT EXISTS promotion_usage_order_id_idx ON promotion_usage (order_id);
CREATE INDEX IF NOT EXISTS promotion_usage_checkout_ref_idx ON promotion_usage (checkout_reference);
