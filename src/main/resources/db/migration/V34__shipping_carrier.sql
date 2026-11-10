-- Carrier integration: shipments, per-line allocation and tracking webhook inbox.
CREATE TABLE IF NOT EXISTS shipment (
    id                 VARCHAR(50) PRIMARY KEY,
    created_at         TIMESTAMP    NOT NULL DEFAULT (NOW() AT TIME ZONE 'utc'),
    updated_at         TIMESTAMP,
    order_id           VARCHAR(50)  NOT NULL REFERENCES "order"(id) ON DELETE CASCADE,
    carrier            VARCHAR(50)  NOT NULL,
    service            VARCHAR(100),
    tracking_number    VARCHAR(100),
    label_url          TEXT,
    provider_reference VARCHAR(150),
    rate_id            VARCHAR(150),
    rate_amount        DECIMAL(10, 2),
    currency           VARCHAR(3)   NOT NULL DEFAULT 'USD',
    status             VARCHAR(30)  NOT NULL DEFAULT 'PENDING',
    shipped_at         TIMESTAMP,
    delivered_at       TIMESTAMP
);

CREATE INDEX IF NOT EXISTS shipment_order_idx ON shipment(order_id);
CREATE INDEX IF NOT EXISTS shipment_tracking_idx ON shipment(tracking_number);
CREATE INDEX IF NOT EXISTS shipment_provider_reference_idx ON shipment(provider_reference);

CREATE TABLE IF NOT EXISTS shipment_item (
    id            VARCHAR(50) PRIMARY KEY,
    created_at    TIMESTAMP   NOT NULL DEFAULT (NOW() AT TIME ZONE 'utc'),
    updated_at    TIMESTAMP,
    shipment_id   VARCHAR(50) NOT NULL REFERENCES shipment(id) ON DELETE CASCADE,
    order_item_id VARCHAR(50) NOT NULL REFERENCES order_item(id),
    quantity      INTEGER     NOT NULL,
    CONSTRAINT shipment_item_quantity_positive CHECK (quantity > 0),
    CONSTRAINT shipment_item_unique UNIQUE (shipment_id, order_item_id)
);

CREATE INDEX IF NOT EXISTS shipment_item_order_item_idx ON shipment_item(order_item_id);

CREATE TABLE IF NOT EXISTS carrier_webhook_event (
    id           VARCHAR(50) PRIMARY KEY,
    created_at   TIMESTAMP    NOT NULL DEFAULT (NOW() AT TIME ZONE 'utc'),
    updated_at   TIMESTAMP,
    provider     VARCHAR(50)  NOT NULL,
    event_id     VARCHAR(150) NOT NULL,
    event_type   VARCHAR(100) NOT NULL,
    status       VARCHAR(20)  NOT NULL DEFAULT 'RECEIVED',
    payload      TEXT         NOT NULL,
    error        TEXT,
    received_at  TIMESTAMP    NOT NULL DEFAULT (NOW() AT TIME ZONE 'utc'),
    processed_at TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS carrier_webhook_event_provider_event_unique
  ON carrier_webhook_event(provider, event_id);
