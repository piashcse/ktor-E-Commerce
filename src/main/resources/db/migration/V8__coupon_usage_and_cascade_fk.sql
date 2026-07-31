-- V8: Per-user coupon usage tracking (M6) + ON DELETE CASCADE for owned child rows (M7)
-- M6) coupon_usage table records which user applied a coupon to which order.
-- M7) Hard deletes of product/category/brand currently fail with 500 when child rows exist
--     (no ON DELETE CASCADE). This migration rewrites the FK actions:
--       * CASCADE    for child rows owned by the parent (images, inventory, reviews, carts,
--                    wishlists, stock reservations, sub-categories, category->products)
--       * SET NULL   for nullable references that must survive the parent delete (products keep
--                    listing when brand/sub-category/shop is removed)
-- Constraints are located by column via pg_constraint because Postgres auto-names FKs.

CREATE TABLE coupon_usage (
    id          VARCHAR(50) PRIMARY KEY,
    created_at  TIMESTAMP NOT NULL DEFAULT (NOW() AT TIME ZONE 'utc'),
    updated_at  TIMESTAMP,
    coupon_id   VARCHAR(50) NOT NULL REFERENCES coupon(id),
    user_id     VARCHAR(50) NOT NULL REFERENCES "user"(id),
    order_id    VARCHAR(50) REFERENCES "order"(id),
    used_at     TIMESTAMP NOT NULL DEFAULT (NOW() AT TIME ZONE 'utc')
);

CREATE INDEX coupon_usage_coupon_id_idx ON coupon_usage(coupon_id);
CREATE INDEX coupon_usage_user_id_idx ON coupon_usage(user_id);
CREATE INDEX coupon_usage_coupon_user_idx ON coupon_usage(coupon_id, user_id);

-- Helper: drop the auto-named FK on (child.column -> parent.id) if present.
CREATE OR REPLACE FUNCTION drop_fk_if_exists(child_table TEXT, column_name TEXT, parent_table TEXT)
RETURNS void LANGUAGE plpgsql AS $$
DECLARE
    conname TEXT;
BEGIN
    SELECT c.conname INTO conname
    FROM pg_constraint c
    JOIN pg_attribute a
      ON a.attnum = ANY(c.conkey) AND a.attrelid = c.conrelid
    WHERE c.conrelid = child_table::regclass
      AND c.confrelid = parent_table::regclass
      AND c.contype = 'f'
      AND a.attname = column_name;
    IF conname IS NOT NULL THEN
        EXECUTE format('ALTER TABLE %s DROP CONSTRAINT %I', child_table, conname);
    END IF;
END $$;

SELECT drop_fk_if_exists('product_image', 'product_id', 'product');
ALTER TABLE product_image ADD CONSTRAINT product_image_product_id_fkey
    FOREIGN KEY (product_id) REFERENCES product(id) ON DELETE CASCADE;

SELECT drop_fk_if_exists('inventory', 'product_id', 'product');
ALTER TABLE inventory ADD CONSTRAINT inventory_product_id_fkey
    FOREIGN KEY (product_id) REFERENCES product(id) ON DELETE CASCADE;

SELECT drop_fk_if_exists('inventory', 'shop_id', 'shop');
ALTER TABLE inventory ADD CONSTRAINT inventory_shop_id_fkey
    FOREIGN KEY (shop_id) REFERENCES shop(id) ON DELETE CASCADE;

SELECT drop_fk_if_exists('review_rating', 'product_id', 'product');
ALTER TABLE review_rating ADD CONSTRAINT review_rating_product_id_fkey
    FOREIGN KEY (product_id) REFERENCES product(id) ON DELETE CASCADE;

SELECT drop_fk_if_exists('cart_item', 'product_id', 'product');
ALTER TABLE cart_item ADD CONSTRAINT cart_item_product_id_fkey
    FOREIGN KEY (product_id) REFERENCES product(id) ON DELETE CASCADE;

SELECT drop_fk_if_exists('wishlist', 'product_id', 'product');
ALTER TABLE wishlist ADD CONSTRAINT wishlist_product_id_fkey
    FOREIGN KEY (product_id) REFERENCES product(id) ON DELETE CASCADE;

SELECT drop_fk_if_exists('stock_reservation', 'product_id', 'product');
ALTER TABLE stock_reservation ADD CONSTRAINT stock_reservation_product_id_fkey
    FOREIGN KEY (product_id) REFERENCES product(id) ON DELETE CASCADE;

SELECT drop_fk_if_exists('stock_reservation', 'order_item_id', 'order_item');
ALTER TABLE stock_reservation ADD CONSTRAINT stock_reservation_order_item_id_fkey
    FOREIGN KEY (order_item_id) REFERENCES order_item(id) ON DELETE CASCADE;

SELECT drop_fk_if_exists('sub_category', 'category_id', 'category');
ALTER TABLE sub_category ADD CONSTRAINT sub_category_category_id_fkey
    FOREIGN KEY (category_id) REFERENCES category(id) ON DELETE CASCADE;

SELECT drop_fk_if_exists('product', 'category_id', 'category');
ALTER TABLE product ADD CONSTRAINT product_category_id_fkey
    FOREIGN KEY (category_id) REFERENCES category(id) ON DELETE CASCADE;

SELECT drop_fk_if_exists('product', 'sub_category_id', 'sub_category');
ALTER TABLE product ADD CONSTRAINT product_sub_category_id_fkey
    FOREIGN KEY (sub_category_id) REFERENCES sub_category(id) ON DELETE SET NULL;

SELECT drop_fk_if_exists('product', 'brand_id', 'brand');
ALTER TABLE product ADD CONSTRAINT product_brand_id_fkey
    FOREIGN KEY (brand_id) REFERENCES brand(id) ON DELETE SET NULL;

SELECT drop_fk_if_exists('product', 'shop_id', 'shop');
ALTER TABLE product ADD CONSTRAINT product_shop_id_fkey
    FOREIGN KEY (shop_id) REFERENCES shop(id) ON DELETE SET NULL;

DROP FUNCTION drop_fk_if_exists(TEXT, TEXT, TEXT);
