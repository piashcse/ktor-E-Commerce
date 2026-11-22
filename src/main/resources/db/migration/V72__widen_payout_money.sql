-- Entity metadata and production money columns must match. Orders were widened
-- in V19; seller payout rows could still overflow at DECIMAL(10,2).
ALTER TABLE seller_payout ALTER COLUMN sub_total TYPE DECIMAL(12,2);
ALTER TABLE seller_payout ALTER COLUMN commission_amount TYPE DECIMAL(12,2);
ALTER TABLE seller_payout ALTER COLUMN payout_amount TYPE DECIMAL(12,2);
