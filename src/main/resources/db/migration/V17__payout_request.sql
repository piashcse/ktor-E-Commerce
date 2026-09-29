-- V17: seller payout request lifecycle (new columns only; never edit old migrations).
-- No status enum exists, so REQUESTED/APPROVED reuse PENDING plus timestamps:
-- requested_at set = REQUESTED, approved_at set = APPROVED, rejected_at set = REJECTED.
ALTER TABLE seller_payout ADD COLUMN IF NOT EXISTS requested_at TIMESTAMP NULL;
ALTER TABLE seller_payout ADD COLUMN IF NOT EXISTS approved_at TIMESTAMP NULL;
ALTER TABLE seller_payout ADD COLUMN IF NOT EXISTS rejected_at TIMESTAMP NULL;
-- Retry-safe unique guard: NULL keys stay distinct in PG; enforce uniqueness only
-- for non-NULL idempotency keys so double-submitted payout requests become 409s.
ALTER TABLE seller_payout ADD COLUMN IF NOT EXISTS payout_key VARCHAR(100) NULL;
CREATE UNIQUE INDEX IF NOT EXISTS seller_payout_key_not_null
  ON seller_payout(payout_key) WHERE payout_key IS NOT NULL;
CREATE INDEX IF NOT EXISTS seller_payout_requested_at_idx ON seller_payout(requested_at);
