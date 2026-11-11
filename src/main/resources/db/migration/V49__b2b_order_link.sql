-- Link fulfilled B2B purchase orders to their execution order.
ALTER TABLE b2b_purchase_order ADD COLUMN IF NOT EXISTS order_id VARCHAR(50) REFERENCES "order"(id) ON DELETE SET NULL;
CREATE UNIQUE INDEX IF NOT EXISTS b2b_purchase_order_order_unique
  ON b2b_purchase_order(order_id) WHERE order_id IS NOT NULL;
