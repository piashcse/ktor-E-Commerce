ALTER TABLE outbox
    ADD COLUMN IF NOT EXISTS claimed_at TIMESTAMP;

CREATE INDEX IF NOT EXISTS outbox_claimed_at_idx
    ON outbox (claimed_at);
