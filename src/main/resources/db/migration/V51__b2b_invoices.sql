-- B2B net-terms invoices and dunning.

CREATE TABLE IF NOT EXISTS b2b_invoice (
    id                 VARCHAR(50) PRIMARY KEY,
    created_at         TIMESTAMP     NOT NULL DEFAULT (NOW() AT TIME ZONE 'utc'),
    updated_at         TIMESTAMP,
    purchase_order_id  VARCHAR(50)   NOT NULL REFERENCES b2b_purchase_order(id) ON DELETE CASCADE,
    order_id           VARCHAR(50)   REFERENCES "order"(id) ON DELETE SET NULL,
    buyer_user_id      VARCHAR(50)   NOT NULL REFERENCES "user"(id) ON DELETE CASCADE,
    seller_id          VARCHAR(50)   NOT NULL REFERENCES seller(id),
    invoice_number     VARCHAR(50)   NOT NULL,
    status             VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    currency           VARCHAR(3)    NOT NULL DEFAULT 'USD',
    amount             DECIMAL(12, 2) NOT NULL,
    due_date           TIMESTAMP     NOT NULL,
    paid_at            TIMESTAMP,
    provider_reference VARCHAR(150),
    reminder_count     INTEGER       NOT NULL DEFAULT 0,
    last_reminder_at   TIMESTAMP,
    next_reminder_at   TIMESTAMP,
    CONSTRAINT b2b_invoice_amount_positive CHECK (amount > 0)
);

CREATE UNIQUE INDEX IF NOT EXISTS b2b_invoice_po_unique ON b2b_invoice(purchase_order_id);
CREATE UNIQUE INDEX IF NOT EXISTS b2b_invoice_number_unique ON b2b_invoice(invoice_number);
CREATE INDEX IF NOT EXISTS b2b_invoice_buyer_idx ON b2b_invoice(buyer_user_id, status, due_date);
CREATE INDEX IF NOT EXISTS b2b_invoice_seller_idx ON b2b_invoice(seller_id, status, due_date);
CREATE INDEX IF NOT EXISTS b2b_invoice_dunning_idx ON b2b_invoice(status, next_reminder_at);
