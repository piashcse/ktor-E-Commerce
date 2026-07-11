-- V5: Create stock reservation table for checkout flow
CREATE TABLE IF NOT EXISTS stock_reservation (
    id              VARCHAR(50) PRIMARY KEY,
    created_at      TIMESTAMP NOT NULL DEFAULT (NOW() AT TIME ZONE 'utc'),
    updated_at      TIMESTAMP,
    order_id        VARCHAR(50) NOT NULL REFERENCES "order"(id),
    order_item_id   VARCHAR(50) NOT NULL REFERENCES order_item(id),
    product_id      VARCHAR(50) NOT NULL REFERENCES product(id),
    shop_id         VARCHAR(50) REFERENCES shop(id),
    quantity        INTEGER NOT NULL CHECK (quantity > 0),
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    expires_at      TIMESTAMP NOT NULL
);

CREATE INDEX stock_reservation_order_id_idx ON stock_reservation(order_id);
CREATE INDEX stock_reservation_status_idx ON stock_reservation(status);
CREATE INDEX stock_reservation_expires_at_idx ON stock_reservation(expires_at);
