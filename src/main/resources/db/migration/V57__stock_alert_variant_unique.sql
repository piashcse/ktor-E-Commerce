-- Allow one back-in-stock subscription per variant. The old
-- (user_id, product_id) index overwrote the first variant.
DROP INDEX IF EXISTS stock_alert_user_product_unique;

CREATE UNIQUE INDEX IF NOT EXISTS stock_alert_user_product_variant_unique
    ON stock_alert (user_id, product_id, variant_id) NULLS NOT DISTINCT;
