-- POS cash-drawer sessions.

CREATE TABLE IF NOT EXISTS pos_session (
    id             VARCHAR(50) PRIMARY KEY,
    created_at     TIMESTAMP     NOT NULL DEFAULT (NOW() AT TIME ZONE 'utc'),
    updated_at     TIMESTAMP,
    shop_id        VARCHAR(50)   NOT NULL REFERENCES shop(id) ON DELETE CASCADE,
    warehouse_id   VARCHAR(50)   REFERENCES warehouse(id) ON DELETE SET NULL,
    opened_by      VARCHAR(50)   NOT NULL REFERENCES "user"(id),
    status         VARCHAR(20)   NOT NULL DEFAULT 'OPEN',
    opening_float  DECIMAL(12, 2) NOT NULL DEFAULT 0,
    cash_sales     DECIMAL(12, 2) NOT NULL DEFAULT 0,
    card_sales     DECIMAL(12, 2) NOT NULL DEFAULT 0,
    expected_cash  DECIMAL(12, 2),
    counted_cash   DECIMAL(12, 2),
    variance       DECIMAL(12, 2),
    opened_at      TIMESTAMP     NOT NULL DEFAULT (NOW() AT TIME ZONE 'utc'),
    closed_at      TIMESTAMP,
    notes          TEXT,
    close_notes    TEXT,
    CONSTRAINT pos_session_float_non_negative CHECK (opening_float >= 0)
);

CREATE INDEX IF NOT EXISTS pos_session_shop_status_idx ON pos_session(shop_id, status);
CREATE INDEX IF NOT EXISTS pos_session_opened_idx ON pos_session(opened_at);

ALTER TABLE "order" ADD COLUMN IF NOT EXISTS pos_session_id VARCHAR(50) REFERENCES pos_session(id) ON DELETE SET NULL;
CREATE INDEX IF NOT EXISTS order_pos_session_idx ON "order"(pos_session_id);
