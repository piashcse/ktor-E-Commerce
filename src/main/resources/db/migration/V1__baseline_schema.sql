-- V1: Baseline schema matching Exposed entity definitions
-- This is the authoritative schema source. Exposed DAO classes use these tables.

-- ============================================================
-- TABLES WITH NO FOREIGN KEY DEPENDENCIES
-- ============================================================

CREATE TABLE "user" (
    id          VARCHAR(50) PRIMARY KEY,
    created_at  TIMESTAMP NOT NULL DEFAULT (NOW() AT TIME ZONE 'utc'),
    updated_at  TIMESTAMP,
    email       VARCHAR(255) NOT NULL,
    user_type   INTEGER NOT NULL,
    password    VARCHAR(200) NOT NULL,
    otp_code    VARCHAR(6),
    otp_expiry  TIMESTAMP,
    reset_otp_code     VARCHAR(6),
    reset_otp_expiry   TIMESTAMP,
    is_verified BOOLEAN NOT NULL DEFAULT FALSE,
    is_active   BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE UNIQUE INDEX email_userType_idx ON "user"(email, user_type);

CREATE TABLE shop_category (
    id          VARCHAR(50) PRIMARY KEY,
    created_at  TIMESTAMP NOT NULL DEFAULT (NOW() AT TIME ZONE 'utc'),
    updated_at  TIMESTAMP,
    name        TEXT NOT NULL
);

CREATE TABLE login_attempt (
    id            VARCHAR(50) PRIMARY KEY,
    created_at    TIMESTAMP NOT NULL DEFAULT (NOW() AT TIME ZONE 'utc'),
    updated_at    TIMESTAMP,
    email         VARCHAR(255) NOT NULL,
    user_type     INTEGER NOT NULL,
    ip_address    VARCHAR(45),
    attempt_count INTEGER NOT NULL DEFAULT 0,
    locked_until  TIMESTAMP
);

CREATE UNIQUE INDEX login_attempt_email_user_type_idx ON login_attempt(email, user_type);

CREATE TABLE shipping_method (
    id            VARCHAR(50) PRIMARY KEY,
    created_at    TIMESTAMP NOT NULL DEFAULT (NOW() AT TIME ZONE 'utc'),
    updated_at    TIMESTAMP,
    name          VARCHAR(50) NOT NULL,
    type          VARCHAR(50),
    price         DOUBLE PRECISION NOT NULL,
    delivery_time VARCHAR(50)
);

CREATE TABLE brand (
    id          VARCHAR(50) PRIMARY KEY,
    created_at  TIMESTAMP NOT NULL DEFAULT (NOW() AT TIME ZONE 'utc'),
    updated_at  TIMESTAMP,
    name        TEXT NOT NULL,
    logo        TEXT
);

CREATE TABLE policy_documents (
    id             VARCHAR(50) PRIMARY KEY,
    created_at     TIMESTAMP NOT NULL DEFAULT (NOW() AT TIME ZONE 'utc'),
    updated_at     TIMESTAMP,
    title          VARCHAR(255) NOT NULL,
    type           INTEGER NOT NULL,
    content        TEXT NOT NULL,
    version        VARCHAR(50) NOT NULL,
    effective_date TIMESTAMP NOT NULL,
    is_active      BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE category (
    id          VARCHAR(50) PRIMARY KEY,
    created_at  TIMESTAMP NOT NULL DEFAULT (NOW() AT TIME ZONE 'utc'),
    updated_at  TIMESTAMP,
    name        TEXT NOT NULL,
    image       TEXT
);

CREATE TABLE blacklisted_token (
    id              VARCHAR(50) PRIMARY KEY,
    created_at      TIMESTAMP NOT NULL DEFAULT (NOW() AT TIME ZONE 'utc'),
    updated_at      TIMESTAMP,
    token           VARCHAR(1000) NOT NULL,
    blacklisted_at  TIMESTAMP NOT NULL
);

CREATE UNIQUE INDEX blacklisted_token_token_idx ON blacklisted_token(token);

-- ============================================================
-- TABLES WITH FOREIGN KEY DEPENDENCIES (level 1)
-- ============================================================

CREATE TABLE user_profile (
    id                   VARCHAR(50) PRIMARY KEY,
    created_at           TIMESTAMP NOT NULL DEFAULT (NOW() AT TIME ZONE 'utc'),
    updated_at           TIMESTAMP,
    user_id              VARCHAR(50) NOT NULL REFERENCES "user"(id),
    image                TEXT,
    first_name           TEXT,
    last_name            TEXT,
    mobile               TEXT,
    fax_number           TEXT,
    street_address       TEXT,
    city                 TEXT,
    state                TEXT,
    country              TEXT,
    identification_type  TEXT,
    identification_no    TEXT,
    occupation           TEXT,
    post_code            TEXT,
    gender               TEXT,
    date_of_birth        DATE,
    bio                  TEXT,
    is_active            BOOLEAN NOT NULL DEFAULT TRUE,
    verified             BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE shop (
    id            VARCHAR(50) PRIMARY KEY,
    created_at    TIMESTAMP NOT NULL DEFAULT (NOW() AT TIME ZONE 'utc'),
    updated_at    TIMESTAMP,
    user_id       VARCHAR(50) NOT NULL REFERENCES "user"(id),
    category_id   VARCHAR(50) NOT NULL REFERENCES shop_category(id),
    name          TEXT NOT NULL,
    description   TEXT,
    address       TEXT,
    phone         VARCHAR(20),
    email         VARCHAR(255),
    logo          VARCHAR(500),
    cover_image   VARCHAR(500),
    status        INTEGER NOT NULL DEFAULT 0,
    rating        DECIMAL(3,2) NOT NULL DEFAULT 0.00,
    total_reviews INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE seller (
    id                          VARCHAR(50) PRIMARY KEY,
    created_at                  TIMESTAMP NOT NULL DEFAULT (NOW() AT TIME ZONE 'utc'),
    updated_at                  TIMESTAMP,
    user_id                     VARCHAR(50) NOT NULL REFERENCES "user"(id),
    shop_id                     VARCHAR(50) REFERENCES shop(id),
    business_name               VARCHAR(255),
    business_registration_number VARCHAR(100),
    tax_id                      VARCHAR(100),
    bank_account_number         VARCHAR(50),
    bank_name                   VARCHAR(100),
    bank_routing_number         VARCHAR(50),
    commission_rate             DECIMAL(5,2) NOT NULL DEFAULT 10.00,
    status                      INTEGER NOT NULL DEFAULT 0,
    total_sales                 DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    total_commission            DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    approved_at                 TIMESTAMP,
    suspended_at                TIMESTAMP,
    terminated_at               TIMESTAMP
);

CREATE TABLE sub_category (
    id          VARCHAR(50) PRIMARY KEY,
    created_at  TIMESTAMP NOT NULL DEFAULT (NOW() AT TIME ZONE 'utc'),
    updated_at  TIMESTAMP,
    category_id VARCHAR(50) NOT NULL REFERENCES category(id),
    name        TEXT NOT NULL,
    image       TEXT
);

-- ============================================================
-- TABLES WITH FOREIGN KEY DEPENDENCIES (level 2)
-- ============================================================

CREATE TABLE product (
    id                  VARCHAR(50) PRIMARY KEY,
    created_at          TIMESTAMP NOT NULL DEFAULT (NOW() AT TIME ZONE 'utc'),
    updated_at          TIMESTAMP,
    user_id             VARCHAR(50) NOT NULL REFERENCES "user"(id),
    shop_id             VARCHAR(50) REFERENCES shop(id),
    name                TEXT NOT NULL,
    description         TEXT NOT NULL,
    category_id         VARCHAR(50) NOT NULL REFERENCES category(id),
    sub_category_id     VARCHAR(50) REFERENCES sub_category(id),
    brand_id            VARCHAR(50) REFERENCES brand(id),
    sku                 VARCHAR(100) NOT NULL,
    barcode             VARCHAR(100),
    weight              DECIMAL(10,3),
    dimensions          VARCHAR(100),
    min_order_quantity  INTEGER NOT NULL DEFAULT 1,
    price               DECIMAL(10,2) NOT NULL,
    discount_price      DECIMAL(10,2),
    discount_percentage DECIMAL(5,2),
    video_link          TEXT,
    hot_deal            BOOLEAN NOT NULL DEFAULT FALSE,
    featured            BOOLEAN NOT NULL DEFAULT FALSE,
    best_seller         BOOLEAN NOT NULL DEFAULT FALSE,
    new_product         BOOLEAN NOT NULL DEFAULT FALSE,
    free_shipping       BOOLEAN NOT NULL DEFAULT FALSE,
    status              INTEGER NOT NULL DEFAULT 0,
    view_count          INTEGER NOT NULL DEFAULT 0,
    rating              DECIMAL(3,2) NOT NULL DEFAULT 0.00,
    total_reviews       INTEGER NOT NULL DEFAULT 0,
    total_sales         INTEGER NOT NULL DEFAULT 0,
    stock_quantity      INTEGER NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX product_sku_idx ON product(sku);
CREATE INDEX product_user_id_idx ON product(user_id);
CREATE INDEX product_shop_id_idx ON product(shop_id);
CREATE INDEX product_category_id_idx ON product(category_id);
CREATE INDEX product_barcode_idx ON product(barcode);
CREATE INDEX product_hot_deal_idx ON product(hot_deal);
CREATE INDEX product_featured_idx ON product(featured);
CREATE INDEX product_best_seller_idx ON product(best_seller);
CREATE INDEX product_status_idx ON product(status);

CREATE TABLE product_image (
    id          VARCHAR(50) PRIMARY KEY,
    created_at  TIMESTAMP NOT NULL DEFAULT (NOW() AT TIME ZONE 'utc'),
    updated_at  TIMESTAMP,
    product_id  VARCHAR(50) NOT NULL REFERENCES product(id),
    image_url   VARCHAR(500) NOT NULL,
    sort_order  INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX product_image_product_id_idx ON product_image(product_id);

CREATE TABLE coupon (
    id                  VARCHAR(50) PRIMARY KEY,
    created_at          TIMESTAMP NOT NULL DEFAULT (NOW() AT TIME ZONE 'utc'),
    updated_at          TIMESTAMP,
    code                VARCHAR(50) NOT NULL,
    discount_type       INTEGER NOT NULL,
    discount_value      DOUBLE PRECISION NOT NULL,
    min_order_amount    DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    max_discount_amount DOUBLE PRECISION,
    start_date          TIMESTAMP NOT NULL,
    end_date            TIMESTAMP NOT NULL,
    usage_limit         INTEGER,
    usage_count         INTEGER NOT NULL DEFAULT 0,
    is_active           BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE UNIQUE INDEX coupon_code_idx ON coupon(code);

CREATE TABLE shipping_address (
    id              VARCHAR(50) PRIMARY KEY,
    created_at      TIMESTAMP NOT NULL DEFAULT (NOW() AT TIME ZONE 'utc'),
    updated_at      TIMESTAMP,
    user_id         VARCHAR(50) NOT NULL REFERENCES "user"(id),
    first_name      VARCHAR(50) NOT NULL,
    last_name       VARCHAR(50) NOT NULL,
    email           VARCHAR(50) NOT NULL,
    phone_number    VARCHAR(20) NOT NULL,
    street_address  VARCHAR(150) NOT NULL,
    city            VARCHAR(50) NOT NULL,
    state           VARCHAR(50),
    country         VARCHAR(50) NOT NULL,
    zip_code        VARCHAR(20) NOT NULL,
    is_default      BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX shipping_address_user_id_idx ON shipping_address(user_id);

CREATE TABLE "order" (
    id                VARCHAR(50) PRIMARY KEY,
    created_at        TIMESTAMP NOT NULL DEFAULT (NOW() AT TIME ZONE 'utc'),
    updated_at        TIMESTAMP,
    user_id           VARCHAR(50) NOT NULL REFERENCES "user"(id),
    shop_id           VARCHAR(50) REFERENCES shop(id),
    order_number      VARCHAR(50) NOT NULL,
    idempotency_key   VARCHAR(100),
    sub_total         DECIMAL(10,2) NOT NULL,
    shipping_cost     DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    tax_amount        DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    discount_amount   DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    coupon_code       VARCHAR(50),
    total             DECIMAL(10,2) NOT NULL,
    currency          VARCHAR(3) NOT NULL DEFAULT 'USD',
    payment_method    INTEGER,
    payment_status    INTEGER NOT NULL DEFAULT 0,
    status            INTEGER NOT NULL DEFAULT 0,
    notes             TEXT,
    shipping_method   VARCHAR(50),
    shipping_address  TEXT,
    billing_address   TEXT,
    shipping_date     TIMESTAMP,
    delivered_date    TIMESTAMP,
    canceled_date     TIMESTAMP,
    completed_date    TIMESTAMP
);

CREATE UNIQUE INDEX order_order_number_idx ON "order"(order_number);
CREATE UNIQUE INDEX order_idempotency_key_idx ON "order"(idempotency_key);
CREATE INDEX order_user_id_idx ON "order"(user_id);
CREATE INDEX order_shop_id_idx ON "order"(shop_id);
CREATE INDEX order_status_idx ON "order"(status);
CREATE INDEX order_created_at_idx ON "order"(created_at);

CREATE TABLE order_item (
    id              VARCHAR(50) PRIMARY KEY,
    created_at      TIMESTAMP NOT NULL DEFAULT (NOW() AT TIME ZONE 'utc'),
    updated_at      TIMESTAMP,
    order_id        VARCHAR(50) NOT NULL REFERENCES "order"(id),
    product_id      VARCHAR(50) NOT NULL REFERENCES product(id),
    shop_id         VARCHAR(50) NOT NULL REFERENCES shop(id),
    quantity        INTEGER NOT NULL,
    price           DECIMAL(10,2) NOT NULL,
    discount_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    tax_amount      DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    total           DECIMAL(10,2) NOT NULL,
    sku             VARCHAR(100),
    product_name    VARCHAR(255)
);

CREATE TABLE order_status_history (
    id          VARCHAR(50) PRIMARY KEY,
    created_at  TIMESTAMP NOT NULL DEFAULT (NOW() AT TIME ZONE 'utc'),
    updated_at  TIMESTAMP,
    order_id    VARCHAR(50) NOT NULL REFERENCES "order"(id),
    status      INTEGER NOT NULL,
    notes       TEXT,
    changed_by  VARCHAR(50) REFERENCES "user"(id)
);

CREATE TABLE payment (
    id              VARCHAR(50) PRIMARY KEY,
    created_at      TIMESTAMP NOT NULL DEFAULT (NOW() AT TIME ZONE 'utc'),
    updated_at      TIMESTAMP,
    order_id        VARCHAR(50) NOT NULL REFERENCES "order"(id),
    user_id         VARCHAR(50) NOT NULL REFERENCES "user"(id),
    amount          BIGINT NOT NULL,
    status          INTEGER NOT NULL DEFAULT 0,
    payment_method  INTEGER NOT NULL,
    transaction_id  VARCHAR(100)
);

CREATE TABLE cart_item (
    id          VARCHAR(50) PRIMARY KEY,
    created_at  TIMESTAMP NOT NULL DEFAULT (NOW() AT TIME ZONE 'utc'),
    updated_at  TIMESTAMP,
    user_id     VARCHAR(50) NOT NULL REFERENCES "user"(id),
    product_id  VARCHAR(50) NOT NULL REFERENCES product(id),
    quantity    INTEGER NOT NULL DEFAULT 1
);

CREATE INDEX cart_item_user_id_idx ON cart_item(user_id);
CREATE INDEX cart_item_user_product_idx ON cart_item(user_id, product_id);

CREATE TABLE wishlist (
    id          VARCHAR(50) PRIMARY KEY,
    created_at  TIMESTAMP NOT NULL DEFAULT (NOW() AT TIME ZONE 'utc'),
    updated_at  TIMESTAMP,
    user_id     VARCHAR(50) NOT NULL REFERENCES "user"(id),
    product_id  VARCHAR(50) NOT NULL REFERENCES product(id)
);

CREATE INDEX wishlist_user_id_idx ON wishlist(user_id);
CREATE INDEX wishlist_user_product_idx ON wishlist(user_id, product_id);

CREATE TABLE inventory (
    id                  VARCHAR(50) PRIMARY KEY,
    created_at          TIMESTAMP NOT NULL DEFAULT (NOW() AT TIME ZONE 'utc'),
    updated_at          TIMESTAMP,
    product_id          VARCHAR(50) NOT NULL REFERENCES product(id),
    shop_id             VARCHAR(50) NOT NULL REFERENCES shop(id),
    stock_quantity      INTEGER NOT NULL DEFAULT 0,
    reserved_quantity   INTEGER NOT NULL DEFAULT 0,
    minimum_stock_level INTEGER NOT NULL DEFAULT 10,
    maximum_stock_level INTEGER NOT NULL DEFAULT 1000,
    status              INTEGER NOT NULL DEFAULT 0,
    last_restocked      TIMESTAMP
);

CREATE UNIQUE INDEX inventory_product_shop_idx ON inventory(product_id, shop_id);

CREATE TABLE review_rating (
    id                   VARCHAR(50) PRIMARY KEY,
    created_at           TIMESTAMP NOT NULL DEFAULT (NOW() AT TIME ZONE 'utc'),
    updated_at           TIMESTAMP,
    user_id              VARCHAR(50) NOT NULL REFERENCES "user"(id),
    product_id           VARCHAR(50) NOT NULL REFERENCES product(id),
    review_text          VARCHAR(500) NOT NULL,
    rating               INTEGER NOT NULL CHECK (rating >= 1 AND rating <= 5),
    title                VARCHAR(200),
    is_verified_purchase BOOLEAN NOT NULL DEFAULT FALSE,
    helpful_count        INTEGER NOT NULL DEFAULT 0,
    not_helpful_count    INTEGER NOT NULL DEFAULT 0,
    status               INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX review_rating_user_id_idx ON review_rating(user_id);
CREATE INDEX review_rating_product_id_idx ON review_rating(product_id);

CREATE TABLE refresh_token (
    id          VARCHAR(50) PRIMARY KEY,
    created_at  TIMESTAMP NOT NULL DEFAULT (NOW() AT TIME ZONE 'utc'),
    updated_at  TIMESTAMP,
    user_id     VARCHAR(50) NOT NULL REFERENCES "user"(id),
    token_hash  VARCHAR(255) NOT NULL,
    expires_at  TIMESTAMP NOT NULL,
    revoked_at  TIMESTAMP
);

CREATE UNIQUE INDEX refresh_token_token_hash_idx ON refresh_token(token_hash);

CREATE TABLE refund_request (
    id              VARCHAR(50) PRIMARY KEY,
    created_at      TIMESTAMP NOT NULL DEFAULT (NOW() AT TIME ZONE 'utc'),
    updated_at      TIMESTAMP,
    order_item_id   VARCHAR(50) NOT NULL REFERENCES order_item(id),
    user_id         VARCHAR(50) NOT NULL REFERENCES "user"(id),
    order_id        VARCHAR(50) NOT NULL REFERENCES "order"(id),
    reason          TEXT NOT NULL,
    images          VARCHAR(2000),
    status          INTEGER NOT NULL DEFAULT 0,
    refund_amount   DECIMAL(10,2),
    refund_method   INTEGER,
    tracking_number VARCHAR(100),
    requested_at    TIMESTAMP NOT NULL DEFAULT (NOW() AT TIME ZONE 'utc'),
    resolved_at     TIMESTAMP
);

CREATE TABLE policy_consents (
    id            VARCHAR(50) PRIMARY KEY,
    created_at    TIMESTAMP NOT NULL DEFAULT (NOW() AT TIME ZONE 'utc'),
    updated_at    TIMESTAMP,
    user_id       VARCHAR(50) NOT NULL REFERENCES "user"(id),
    policy_id     VARCHAR(50) NOT NULL REFERENCES policy_documents(id),
    consent_date  TIMESTAMP,
    ip_address    VARCHAR(50),
    user_agent    VARCHAR(255)
);

CREATE TABLE audit_log (
    id            VARCHAR(50) PRIMARY KEY,
    created_at    TIMESTAMP NOT NULL DEFAULT (NOW() AT TIME ZONE 'utc'),
    updated_at    TIMESTAMP,
    actor_id      VARCHAR(50) NOT NULL REFERENCES "user"(id),
    actor_email   VARCHAR(255) NOT NULL,
    actor_role    VARCHAR(50) NOT NULL,
    action        VARCHAR(100) NOT NULL,
    resource_type VARCHAR(100) NOT NULL,
    resource_id   VARCHAR(100),
    details       TEXT,
    ip_address    VARCHAR(45),
    user_agent    TEXT,
    outcome       VARCHAR(20) NOT NULL DEFAULT 'SUCCESS',
    executed_at   TIMESTAMP NOT NULL DEFAULT (NOW() AT TIME ZONE 'utc')
);

CREATE INDEX audit_log_actor_id_idx ON audit_log(actor_id);
CREATE INDEX audit_log_action_idx ON audit_log(action);
CREATE INDEX audit_log_resource_type_idx ON audit_log(resource_type);
CREATE INDEX audit_log_outcome_idx ON audit_log(outcome);
CREATE INDEX audit_log_executed_at_idx ON audit_log(executed_at);
