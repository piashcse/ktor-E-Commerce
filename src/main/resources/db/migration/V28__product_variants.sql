-- V28: product variants, options and variant-level inventory.

CREATE TABLE IF NOT EXISTS product_variant (
    id                  VARCHAR(50) PRIMARY KEY,
    created_at          TIMESTAMP NOT NULL DEFAULT (NOW() AT TIME ZONE 'utc'),
    updated_at          TIMESTAMP,
    product_id          VARCHAR(50) NOT NULL REFERENCES product(id) ON DELETE CASCADE,
    sku                 VARCHAR(100) NOT NULL UNIQUE,
    name                VARCHAR(150) NOT NULL,
    price               NUMERIC(10, 2) NOT NULL,
    discount_price      NUMERIC(10, 2),
    discount_percentage NUMERIC(5, 2),
    barcode             VARCHAR(100),
    weight              NUMERIC(10, 3),
    dimensions          VARCHAR(100),
    image_url           VARCHAR(500),
    status              VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    is_default          BOOLEAN NOT NULL DEFAULT FALSE,
    sort_order          INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS product_variant_product_idx ON product_variant(product_id);
CREATE INDEX IF NOT EXISTS product_variant_status_idx ON product_variant(status);
CREATE UNIQUE INDEX IF NOT EXISTS product_variant_one_default_idx
  ON product_variant(product_id) WHERE is_default = TRUE;

CREATE TABLE IF NOT EXISTS product_option (
    id          VARCHAR(50) PRIMARY KEY,
    created_at  TIMESTAMP NOT NULL DEFAULT (NOW() AT TIME ZONE 'utc'),
    updated_at  TIMESTAMP,
    product_id  VARCHAR(50) NOT NULL REFERENCES product(id) ON DELETE CASCADE,
    name        VARCHAR(100) NOT NULL,
    sort_order  INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS product_option_product_idx ON product_option(product_id);
CREATE UNIQUE INDEX IF NOT EXISTS product_option_product_name_idx ON product_option(product_id, name);

CREATE TABLE IF NOT EXISTS product_option_value (
    id          VARCHAR(50) PRIMARY KEY,
    created_at  TIMESTAMP NOT NULL DEFAULT (NOW() AT TIME ZONE 'utc'),
    updated_at  TIMESTAMP,
    option_id   VARCHAR(50) NOT NULL REFERENCES product_option(id) ON DELETE CASCADE,
    value       VARCHAR(100) NOT NULL,
    sort_order  INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS product_option_value_option_idx ON product_option_value(option_id);
CREATE UNIQUE INDEX IF NOT EXISTS product_option_value_option_value_idx ON product_option_value(option_id, value);

CREATE TABLE IF NOT EXISTS product_variant_option (
    id              VARCHAR(50) PRIMARY KEY,
    created_at      TIMESTAMP NOT NULL DEFAULT (NOW() AT TIME ZONE 'utc'),
    updated_at      TIMESTAMP,
    variant_id      VARCHAR(50) NOT NULL REFERENCES product_variant(id) ON DELETE CASCADE,
    option_id       VARCHAR(50) NOT NULL REFERENCES product_option(id) ON DELETE CASCADE,
    option_value_id VARCHAR(50) NOT NULL REFERENCES product_option_value(id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS product_variant_option_variant_idx ON product_variant_option(variant_id);
CREATE UNIQUE INDEX IF NOT EXISTS product_variant_option_variant_option_idx
  ON product_variant_option(variant_id, option_id);

-- Cart lines can target a variant. Replace the old (user, product) constraint
-- with partial indexes so a user can hold several variants of one product.
ALTER TABLE cart_item ADD COLUMN IF NOT EXISTS variant_id VARCHAR(50) REFERENCES product_variant(id) ON DELETE CASCADE;
ALTER TABLE cart_item DROP CONSTRAINT IF EXISTS cart_item_user_product_unique;
CREATE UNIQUE INDEX IF NOT EXISTS cart_item_user_product_no_variant_idx
  ON cart_item(user_id, product_id) WHERE variant_id IS NULL;
CREATE UNIQUE INDEX IF NOT EXISTS cart_item_user_product_variant_idx
  ON cart_item(user_id, product_id, variant_id) WHERE variant_id IS NOT NULL;

-- Order history keeps the purchased variant snapshot.
ALTER TABLE order_item ADD COLUMN IF NOT EXISTS variant_id VARCHAR(50) REFERENCES product_variant(id) ON DELETE SET NULL;
ALTER TABLE order_item ADD COLUMN IF NOT EXISTS variant_name VARCHAR(255);

-- Reservations reference the variant so expiry/cancellation restores the right bucket.
ALTER TABLE stock_reservation ADD COLUMN IF NOT EXISTS variant_id VARCHAR(50) REFERENCES product_variant(id) ON DELETE SET NULL;

-- Inventory is tracked per variant; NULL keeps legacy product-level rows.
ALTER TABLE inventory ADD COLUMN IF NOT EXISTS variant_id VARCHAR(50) REFERENCES product_variant(id) ON DELETE CASCADE;
DROP INDEX IF EXISTS inventory_product_shop_warehouse_idx;
CREATE UNIQUE INDEX IF NOT EXISTS inventory_product_shop_warehouse_no_variant_idx
  ON inventory(product_id, shop_id, warehouse_id) WHERE variant_id IS NULL;
CREATE UNIQUE INDEX IF NOT EXISTS inventory_product_shop_warehouse_variant_idx
  ON inventory(product_id, shop_id, warehouse_id, variant_id) WHERE variant_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS inventory_variant_idx ON inventory(variant_id);
