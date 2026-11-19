CREATE TABLE IF NOT EXISTS payment_charge_lock (
    id VARCHAR(50) PRIMARY KEY,
    order_id VARCHAR(50) NOT NULL UNIQUE REFERENCES "order"(id) ON DELETE CASCADE,
    owner VARCHAR(50) NOT NULL,
    locked_until TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS payment_charge_lock_locked_until_idx
    ON payment_charge_lock(locked_until);
