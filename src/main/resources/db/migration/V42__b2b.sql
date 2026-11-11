-- B2B accounts, quotes, purchase orders and net terms.
CREATE TABLE IF NOT EXISTS b2b_account (
    id                 VARCHAR(50) PRIMARY KEY,
    created_at         TIMESTAMP     NOT NULL DEFAULT (NOW() AT TIME ZONE 'utc'),
    updated_at         TIMESTAMP,
    user_id            VARCHAR(50)   NOT NULL REFERENCES "user"(id) ON DELETE CASCADE,
    company_name       VARCHAR(200)  NOT NULL,
    tax_id             VARCHAR(50),
    contact_name       VARCHAR(150),
    phone              VARCHAR(30),
    credit_limit       DECIMAL(12, 2) NOT NULL DEFAULT 0,
    payment_terms_days INTEGER       NOT NULL DEFAULT 30,
    status             VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    approved_by        VARCHAR(50)   REFERENCES "user"(id),
    approved_at        TIMESTAMP,
    CONSTRAINT b2b_credit_non_negative CHECK (credit_limit >= 0),
    CONSTRAINT b2b_terms_positive CHECK (payment_terms_days > 0)
);

CREATE UNIQUE INDEX IF NOT EXISTS b2b_account_user_unique ON b2b_account(user_id);
CREATE INDEX IF NOT EXISTS b2b_account_status_idx ON b2b_account(status);

CREATE TABLE IF NOT EXISTS b2b_quote (
    id             VARCHAR(50) PRIMARY KEY,
    created_at     TIMESTAMP     NOT NULL DEFAULT (NOW() AT TIME ZONE 'utc'),
    updated_at     TIMESTAMP,
    buyer_user_id  VARCHAR(50)   NOT NULL REFERENCES "user"(id) ON DELETE CASCADE,
    seller_id      VARCHAR(50)   NOT NULL REFERENCES seller(id),
    shop_id        VARCHAR(50)   REFERENCES shop(id) ON DELETE SET NULL,
    status         VARCHAR(20)   NOT NULL DEFAULT 'REQUESTED',
    currency       VARCHAR(3)    NOT NULL DEFAULT 'USD',
    subtotal       DECIMAL(12, 2) NOT NULL DEFAULT 0,
    tax_amount     DECIMAL(12, 2) NOT NULL DEFAULT 0,
    total          DECIMAL(12, 2) NOT NULL DEFAULT 0,
    notes          TEXT,
    valid_until    TIMESTAMP,
    responded_at   TIMESTAMP
);

CREATE INDEX IF NOT EXISTS b2b_quote_buyer_idx ON b2b_quote(buyer_user_id, created_at);
CREATE INDEX IF NOT EXISTS b2b_quote_seller_idx ON b2b_quote(seller_id, status);

CREATE TABLE IF NOT EXISTS b2b_quote_item (
    id         VARCHAR(50) PRIMARY KEY,
    created_at TIMESTAMP    NOT NULL DEFAULT (NOW() AT TIME ZONE 'utc'),
    updated_at TIMESTAMP,
    quote_id   VARCHAR(50)  NOT NULL REFERENCES b2b_quote(id) ON DELETE CASCADE,
    product_id VARCHAR(50)  NOT NULL REFERENCES product(id),
    quantity   INTEGER      NOT NULL,
    unit_price DECIMAL(10, 2) NOT NULL,
    total      DECIMAL(12, 2) NOT NULL,
    CONSTRAINT b2b_quote_item_qty_positive CHECK (quantity > 0)
);

CREATE INDEX IF NOT EXISTS b2b_quote_item_quote_idx ON b2b_quote_item(quote_id);

CREATE TABLE IF NOT EXISTS b2b_purchase_order (
    id              VARCHAR(50) PRIMARY KEY,
    created_at      TIMESTAMP     NOT NULL DEFAULT (NOW() AT TIME ZONE 'utc'),
    updated_at      TIMESTAMP,
    quote_id        VARCHAR(50)   NOT NULL REFERENCES b2b_quote(id),
    buyer_user_id   VARCHAR(50)   NOT NULL REFERENCES "user"(id) ON DELETE CASCADE,
    seller_id       VARCHAR(50)   NOT NULL REFERENCES seller(id),
    shop_id         VARCHAR(50)   REFERENCES shop(id) ON DELETE SET NULL,
    po_number       VARCHAR(50)   NOT NULL,
    status          VARCHAR(20)   NOT NULL DEFAULT 'OPEN',
    currency        VARCHAR(3)    NOT NULL DEFAULT 'USD',
    total           DECIMAL(12, 2) NOT NULL,
    net_terms_days  INTEGER       NOT NULL DEFAULT 30,
    due_date        TIMESTAMP,
    fulfilled_at    TIMESTAMP,
    canceled_at     TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS b2b_purchase_order_quote_unique ON b2b_purchase_order(quote_id);
CREATE UNIQUE INDEX IF NOT EXISTS b2b_purchase_order_number_unique ON b2b_purchase_order(po_number);
CREATE INDEX IF NOT EXISTS b2b_purchase_order_buyer_idx ON b2b_purchase_order(buyer_user_id, status);
CREATE INDEX IF NOT EXISTS b2b_purchase_order_seller_idx ON b2b_purchase_order(seller_id, status);
