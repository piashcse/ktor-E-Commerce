ALTER TABLE coupon_usage
    ADD COLUMN IF NOT EXISTS checkout_reference VARCHAR(64);

CREATE INDEX IF NOT EXISTS coupon_usage_checkout_ref_idx
    ON coupon_usage (checkout_reference);
