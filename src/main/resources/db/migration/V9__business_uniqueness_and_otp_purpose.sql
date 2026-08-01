-- V9: Enforce business-uniqueness constraints + split OTP attempt counters (R2)
--   1) otp_attempt: counters are per (user, purpose) so registration and reset
--      flows cannot lock each other out (R2-H9).
--   2) cart_item / wishlist / review_rating: unique (user_id, product_id) so a
--      product cannot be added twice and review duplicates are impossible (R2-M4/M5).
--   3) coupon_usage: unique (coupon_id, user_id, order_id) (R2-M5).

-- ── OTP attempt purpose split ──────────────────────────────────────────────
ALTER TABLE otp_attempt ADD COLUMN purpose VARCHAR(20) NOT NULL DEFAULT 'REGISTRATION';
DROP INDEX IF EXISTS otp_attempt_user_id_idx;
CREATE UNIQUE INDEX otp_attempt_user_purpose_idx ON otp_attempt(user_id, purpose);

-- ── cart_item: dedupe then enforce (user_id, product_id) uniqueness ───────
DELETE FROM cart_item a USING cart_item b
WHERE a.user_id = b.user_id AND a.product_id = b.product_id AND a.id > b.id;

DROP INDEX IF EXISTS cart_item_user_product_idx;
CREATE UNIQUE INDEX cart_item_user_product_idx ON cart_item(user_id, product_id);

-- ── wishlist: dedupe then enforce (user_id, product_id) uniqueness ────────
DELETE FROM wishlist a USING wishlist b
WHERE a.user_id = b.user_id AND a.product_id = b.product_id AND a.id > b.id;

DROP INDEX IF EXISTS wishlist_user_product_idx;
CREATE UNIQUE INDEX wishlist_user_product_idx ON wishlist(user_id, product_id);

-- ── review_rating: dedupe then enforce (user_id, product_id) uniqueness ───
DELETE FROM review_rating a USING review_rating b
WHERE a.user_id = b.user_id AND a.product_id = b.product_id AND a.id > b.id;

DROP INDEX IF EXISTS review_rating_user_product_idx;
CREATE UNIQUE INDEX review_rating_user_product_idx ON review_rating(user_id, product_id);

-- ── coupon_usage: enforce (coupon_id, user_id, order_id) uniqueness ───────
DROP INDEX IF EXISTS coupon_usage_coupon_user_idx;
CREATE UNIQUE INDEX coupon_usage_coupon_user_order_idx ON coupon_usage(coupon_id, user_id, order_id);
