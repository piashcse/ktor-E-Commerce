-- Persist the warehouse allocation for each order line so refunds and
-- cancellations restore stock to the warehouses that actually shipped it.
ALTER TABLE stock_reservation
    ADD COLUMN IF NOT EXISTS warehouse_id VARCHAR(50) REFERENCES warehouse(id);

ALTER TABLE stock_reservation
    ADD COLUMN IF NOT EXISTS restored_quantity INT NOT NULL DEFAULT 0;

CREATE INDEX IF NOT EXISTS stock_reservation_warehouse_id_idx
    ON stock_reservation(warehouse_id);
