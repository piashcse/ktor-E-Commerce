-- Storefront funnel events and order attribution.
CREATE TABLE IF NOT EXISTS analytics_event (
    id          VARCHAR(50) PRIMARY KEY,
    created_at  TIMESTAMP    NOT NULL DEFAULT (NOW() AT TIME ZONE 'utc'),
    updated_at  TIMESTAMP,
    session_id  VARCHAR(100) NOT NULL,
    user_id     VARCHAR(50)  REFERENCES "user"(id) ON DELETE SET NULL,
    event_type  VARCHAR(30)  NOT NULL,
    product_id  VARCHAR(50)  REFERENCES product(id) ON DELETE SET NULL,
    order_id    VARCHAR(50)  REFERENCES "order"(id) ON DELETE SET NULL,
    path        VARCHAR(500),
    referrer    VARCHAR(500),
    utm_source  VARCHAR(100),
    utm_medium  VARCHAR(100),
    utm_campaign VARCHAR(150),
    utm_term    VARCHAR(150),
    utm_content VARCHAR(150),
    locale      VARCHAR(10),
    value       DECIMAL(12, 2),
    currency    VARCHAR(3),
    metadata    TEXT,
    occurred_at TIMESTAMP    NOT NULL
);

CREATE INDEX IF NOT EXISTS analytics_event_session_idx ON analytics_event(session_id);
CREATE INDEX IF NOT EXISTS analytics_event_type_time_idx ON analytics_event(event_type, occurred_at);
CREATE INDEX IF NOT EXISTS analytics_event_product_idx ON analytics_event(product_id);
CREATE INDEX IF NOT EXISTS analytics_event_time_idx ON analytics_event(occurred_at);

ALTER TABLE "order" ADD COLUMN IF NOT EXISTS attribution_session_id VARCHAR(100);
ALTER TABLE "order" ADD COLUMN IF NOT EXISTS attribution_source VARCHAR(100);
ALTER TABLE "order" ADD COLUMN IF NOT EXISTS attribution_medium VARCHAR(100);
ALTER TABLE "order" ADD COLUMN IF NOT EXISTS attribution_campaign VARCHAR(150);
ALTER TABLE "order" ADD COLUMN IF NOT EXISTS attribution_referrer VARCHAR(500);

CREATE INDEX IF NOT EXISTS order_attribution_source_idx ON "order"(attribution_source, attribution_campaign);
