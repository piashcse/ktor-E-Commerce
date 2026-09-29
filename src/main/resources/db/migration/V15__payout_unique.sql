-- V15: idempotent payout capture (poller at-least-once relay must not double-pay).
DELETE FROM seller_payout a USING seller_payout b
WHERE a.id > b.id AND a.seller_id = b.seller_id AND a.order_id = b.order_id;
DO $$ BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'seller_payout_seller_order_unique') THEN
    ALTER TABLE seller_payout ADD CONSTRAINT seller_payout_seller_order_unique UNIQUE (seller_id, order_id);
  END IF;
END $$;
