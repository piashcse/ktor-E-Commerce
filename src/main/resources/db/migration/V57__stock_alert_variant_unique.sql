-- Allow one back-in-stock subscription per variant. The old
-- (user_id, product_id) index overwrote the first variant.
--
-- Two partial unique indexes are used instead of PostgreSQL 15's
-- `NULLS NOT DISTINCT` so the migration also works on PostgreSQL 14.
DROP INDEX IF EXISTS stock_alert_user_product_unique;

CREATE UNIQUE INDEX IF NOT EXISTS stock_alert_user_product_variant_unique
    ON stock_alert (user_id, product_id) WHERE variant_id IS NULL;

CREATE UNIQUE INDEX IF NOT EXISTS stock_alert_user_product_variant_nn_unique
    ON stock_alert (user_id, product_id, variant_id) WHERE variant_id IS NOT NULL;
