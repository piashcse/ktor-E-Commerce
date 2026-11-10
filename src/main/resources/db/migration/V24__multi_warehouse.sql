-- V24: multi-warehouse inventory.
CREATE TABLE IF NOT EXISTS warehouse (
    id          VARCHAR(50) PRIMARY KEY,
    created_at  TIMESTAMP NOT NULL DEFAULT (NOW() AT TIME ZONE 'utc'),
    updated_at  TIMESTAMP,
    shop_id     VARCHAR(50) NOT NULL REFERENCES shop(id) ON DELETE CASCADE,
    name        VARCHAR(100) NOT NULL,
    code        VARCHAR(50) NOT NULL,
    address     TEXT,
    city        VARCHAR(100),
    country     VARCHAR(3),
    is_active   BOOLEAN NOT NULL DEFAULT TRUE
);
CREATE INDEX IF NOT EXISTS warehouse_shop_id_idx ON warehouse(shop_id);
CREATE UNIQUE INDEX IF NOT EXISTS warehouse_shop_code_unique ON warehouse(shop_id, code);

-- One default warehouse for every existing shop.
INSERT INTO warehouse (id, shop_id, name, code, is_active)
SELECT 'wh-default-' || id, id, 'Default', 'DEFAULT', TRUE FROM shop
ON CONFLICT DO NOTHING;

ALTER TABLE inventory ADD COLUMN IF NOT EXISTS warehouse_id VARCHAR(50);
UPDATE inventory SET warehouse_id = 'wh-default-' || shop_id WHERE warehouse_id IS NULL;
ALTER TABLE inventory ALTER COLUMN warehouse_id SET NOT NULL;

DO $$
DECLARE
    conname TEXT;
BEGIN
    SELECT c.conname INTO conname
    FROM pg_constraint c
    JOIN pg_attribute a ON a.attnum = ANY(c.conkey) AND a.attrelid = c.conrelid
    WHERE c.conrelid = 'inventory'::regclass
      AND c.contype = 'f'
      AND a.attname = 'warehouse_id';
    IF conname IS NULL THEN
        ALTER TABLE inventory
          ADD CONSTRAINT inventory_warehouse_id_fkey
          FOREIGN KEY (warehouse_id) REFERENCES warehouse(id) ON DELETE CASCADE;
    END IF;
END $$;

DROP INDEX IF EXISTS inventory_product_shop_idx;
CREATE UNIQUE INDEX IF NOT EXISTS inventory_product_shop_warehouse_idx
  ON inventory(product_id, shop_id, warehouse_id);
CREATE INDEX IF NOT EXISTS inventory_warehouse_idx ON inventory(warehouse_id);
