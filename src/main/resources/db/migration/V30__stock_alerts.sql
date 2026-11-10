-- Back-in-stock subscriptions.
CREATE TABLE stock_alert (
    id          VARCHAR(50) PRIMARY KEY,
    created_at  TIMESTAMP   NOT NULL DEFAULT (NOW() AT TIME ZONE 'utc'),
    updated_at  TIMESTAMP,
    user_id     VARCHAR(50) NOT NULL REFERENCES "user"(id) ON DELETE CASCADE,
    product_id  VARCHAR(50) NOT NULL REFERENCES product(id) ON DELETE CASCADE,
    variant_id  VARCHAR(50) REFERENCES product_variant(id) ON DELETE CASCADE,
    notified_at TIMESTAMP
);

CREATE UNIQUE INDEX stock_alert_user_product_unique ON stock_alert(user_id, product_id);
CREATE INDEX stock_alert_product_id_idx ON stock_alert(product_id);
