CREATE TABLE IF NOT EXISTS coupon (
    id VARCHAR(50) PRIMARY KEY,
    code VARCHAR(50) UNIQUE NOT NULL,
    discount_type VARCHAR(20) NOT NULL,
    discount_value DOUBLE PRECISION NOT NULL,
    min_order_amount DOUBLE PRECISION DEFAULT 0.0 NOT NULL,
    max_discount_amount DOUBLE PRECISION NULL,
    start_date TIMESTAMP NOT NULL,
    end_date TIMESTAMP NOT NULL,
    usage_limit INTEGER NULL,
    usage_count INTEGER DEFAULT 0 NOT NULL,
    is_active BOOLEAN DEFAULT TRUE NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NULL
);
