-- V16: idempotent payment capture (gateway at-least-once retries must not double-charge).
DELETE FROM payment a USING payment b
WHERE a.id > b.id AND a.transaction_id = b.transaction_id AND a.transaction_id IS NOT NULL;

-- 1) Partial unique index: NULL transaction ids stay distinct in PG; enforce uniqueness only for non-NULL keys.
CREATE UNIQUE INDEX IF NOT EXISTS payment_transaction_id_not_null
  ON payment(transaction_id) WHERE transaction_id IS NOT NULL;
