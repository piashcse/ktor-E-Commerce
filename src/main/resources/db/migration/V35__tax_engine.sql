-- Tax engine: zones/nexus, product tax classes, inclusive pricing and exemptions.
CREATE TABLE IF NOT EXISTS tax_class (
    id           VARCHAR(50) PRIMARY KEY,
    created_at   TIMESTAMP    NOT NULL DEFAULT (NOW() AT TIME ZONE 'utc'),
    updated_at   TIMESTAMP,
    name         VARCHAR(100) NOT NULL,
    code         VARCHAR(50)  NOT NULL,
    rate         DECIMAL(6, 4) NOT NULL DEFAULT 0,
    is_inclusive BOOLEAN      NOT NULL DEFAULT FALSE,
    is_active    BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT tax_class_rate_check CHECK (rate >= 0 AND rate <= 1)
);

CREATE UNIQUE INDEX IF NOT EXISTS tax_class_code_unique ON tax_class(code);
CREATE UNIQUE INDEX IF NOT EXISTS tax_class_name_unique ON tax_class(name);

CREATE TABLE IF NOT EXISTS tax_zone (
    id         VARCHAR(50) PRIMARY KEY,
    created_at TIMESTAMP   NOT NULL DEFAULT (NOW() AT TIME ZONE 'utc'),
    updated_at TIMESTAMP,
    name       VARCHAR(100) NOT NULL,
    country    VARCHAR(3)   NOT NULL,
    region     VARCHAR(100),
    is_nexus   BOOLEAN      NOT NULL DEFAULT TRUE,
    priority   INTEGER      NOT NULL DEFAULT 0,
    is_active  BOOLEAN      NOT NULL DEFAULT TRUE
);

CREATE UNIQUE INDEX IF NOT EXISTS tax_zone_country_region_unique
  ON tax_zone(country, COALESCE(region, ''));
CREATE INDEX IF NOT EXISTS tax_zone_country_idx ON tax_zone(country);

ALTER TABLE tax_rule ADD COLUMN IF NOT EXISTS zone_id VARCHAR(50) REFERENCES tax_zone(id);
ALTER TABLE tax_rule ADD COLUMN IF NOT EXISTS tax_class_id VARCHAR(50) REFERENCES tax_class(id);
ALTER TABLE tax_rule ADD COLUMN IF NOT EXISTS is_inclusive BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE tax_rule ADD COLUMN IF NOT EXISTS priority INTEGER NOT NULL DEFAULT 0;

-- A rule can now be scoped by zone/class, so the legacy country+category key is too narrow.
DROP INDEX IF EXISTS tax_rule_country_category_unique;
CREATE UNIQUE INDEX IF NOT EXISTS tax_rule_scope_unique
  ON tax_rule(country, COALESCE(category_id, ''), COALESCE(zone_id, ''), COALESCE(tax_class_id, ''));

ALTER TABLE product ADD COLUMN IF NOT EXISTS tax_class_id VARCHAR(50) REFERENCES tax_class(id);
CREATE INDEX IF NOT EXISTS product_tax_class_idx ON product(tax_class_id);

CREATE TABLE IF NOT EXISTS tax_exemption (
    id               VARCHAR(50) PRIMARY KEY,
    created_at       TIMESTAMP    NOT NULL DEFAULT (NOW() AT TIME ZONE 'utc'),
    updated_at       TIMESTAMP,
    user_id          VARCHAR(50) REFERENCES "user"(id) ON DELETE CASCADE,
    business_tax_id  VARCHAR(50),
    country          VARCHAR(3),
    reason           TEXT         NOT NULL,
    certificate_url  VARCHAR(500),
    is_active        BOOLEAN      NOT NULL DEFAULT TRUE,
    expires_at       TIMESTAMP
);

CREATE INDEX IF NOT EXISTS tax_exemption_user_idx ON tax_exemption(user_id);
CREATE INDEX IF NOT EXISTS tax_exemption_business_idx ON tax_exemption(business_tax_id);

ALTER TABLE "order" ADD COLUMN IF NOT EXISTS business_tax_id VARCHAR(50);
ALTER TABLE "order" ADD COLUMN IF NOT EXISTS tax_exempt BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE order_item ADD COLUMN IF NOT EXISTS tax_rate DECIMAL(6, 4) NOT NULL DEFAULT 0;
ALTER TABLE order_item ADD COLUMN IF NOT EXISTS tax_inclusive BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE order_item ADD COLUMN IF NOT EXISTS tax_class_id VARCHAR(50) REFERENCES tax_class(id);
