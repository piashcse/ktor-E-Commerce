-- V25: RMA / return and exchange workflow.
CREATE TABLE IF NOT EXISTS rma (
    id                  VARCHAR(50) PRIMARY KEY,
    created_at          TIMESTAMP NOT NULL DEFAULT (NOW() AT TIME ZONE 'utc'),
    updated_at          TIMESTAMP,
    order_id            VARCHAR(50) NOT NULL REFERENCES "order"(id) ON DELETE CASCADE,
    order_item_id       VARCHAR(50) NOT NULL REFERENCES order_item(id) ON DELETE CASCADE,
    user_id             VARCHAR(50) NOT NULL REFERENCES "user"(id) ON DELETE CASCADE,
    shop_id             VARCHAR(50) REFERENCES shop(id) ON DELETE SET NULL,
    quantity            INTEGER NOT NULL,
    type                VARCHAR(20) NOT NULL,
    status              VARCHAR(20) NOT NULL DEFAULT 'REQUESTED',
    reason              TEXT NOT NULL,
    resolution          VARCHAR(20),
    tracking_number     VARCHAR(100),
    replacement_order_id VARCHAR(50) REFERENCES "order"(id) ON DELETE SET NULL,
    refund_request_id   VARCHAR(50) REFERENCES refund_request(id) ON DELETE SET NULL,
    resolved_at         TIMESTAMP
);
CREATE INDEX IF NOT EXISTS rma_user_id_idx ON rma(user_id);
CREATE INDEX IF NOT EXISTS rma_shop_id_idx ON rma(shop_id);
CREATE INDEX IF NOT EXISTS rma_order_id_idx ON rma(order_id);
CREATE INDEX IF NOT EXISTS rma_status_idx ON rma(status);
