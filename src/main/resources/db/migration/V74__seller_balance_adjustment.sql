-- Seller debt when an already-paid payout is later refunded/charged back.
CREATE TABLE IF NOT EXISTS seller_balance_adjustment (
    id         VARCHAR(50) PRIMARY KEY,
    created_at TIMESTAMP NOT NULL DEFAULT (NOW() AT TIME ZONE 'utc'),
    updated_at TIMESTAMP,
    seller_id  VARCHAR(50) NOT NULL REFERENCES seller(id) ON DELETE CASCADE,
    order_id   VARCHAR(50) REFERENCES "order"(id) ON DELETE SET NULL,
    amount     DECIMAL(12, 2) NOT NULL,
    currency   VARCHAR(3) NOT NULL,
    reason     VARCHAR(100) NOT NULL,
    settled_at TIMESTAMP
);
CREATE INDEX IF NOT EXISTS seller_balance_adjustment_seller_idx
    ON seller_balance_adjustment(seller_id);
