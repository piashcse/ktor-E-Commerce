CREATE UNIQUE INDEX IF NOT EXISTS shipment_order_idempotency_unique
    ON shipment (order_id, idempotency_key)
    WHERE idempotency_key IS NOT NULL;
