-- V10: soft-delete for products + seller payout ledger.

-- Soft-delete: preserve order history / reviews instead of CASCADE hard delete.
ALTER TABLE product ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP NULL;
CREATE INDEX IF NOT EXISTS product_deleted_at_idx ON product(deleted_at);

-- Seller payout ledger: commission captured on PaymentCompletedEvent (payout job pays out later).
CREATE TABLE IF NOT EXISTS seller_payout (
  id VARCHAR(50) PRIMARY KEY,
  created_at TIMESTAMP NOT NULL DEFAULT (NOW() AT TIME ZONE 'utc'),
  updated_at TIMESTAMP,
  seller_id VARCHAR(50) NOT NULL REFERENCES seller(id) ON DELETE CASCADE,
  order_id VARCHAR(50) NOT NULL REFERENCES "order"(id) ON DELETE CASCADE,
  sub_total DECIMAL(10,2) NOT NULL,
  commission_amount DECIMAL(10,2) NOT NULL,
  payout_amount DECIMAL(10,2) NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  paid_at TIMESTAMP NULL
);
CREATE INDEX IF NOT EXISTS seller_payout_seller_idx ON seller_payout(seller_id);
CREATE INDEX IF NOT EXISTS seller_payout_status_idx ON seller_payout(status);
