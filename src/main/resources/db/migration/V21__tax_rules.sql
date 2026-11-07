-- V21: configurable tax rules by country and optional product category.
CREATE TABLE IF NOT EXISTS tax_rule (
    id          VARCHAR(50) PRIMARY KEY,
    created_at  TIMESTAMP NOT NULL DEFAULT (NOW() AT TIME ZONE 'utc'),
    updated_at  TIMESTAMP,
    country     VARCHAR(3) NOT NULL,
    category_id VARCHAR(50) REFERENCES category(id) ON DELETE CASCADE,
    rate        DECIMAL(6,4) NOT NULL,
    is_active   BOOLEAN NOT NULL DEFAULT TRUE
);
CREATE INDEX IF NOT EXISTS tax_rule_country_idx ON tax_rule(country);
CREATE UNIQUE INDEX IF NOT EXISTS tax_rule_country_category_unique
  ON tax_rule(country, COALESCE(category_id, ''))
  WHERE is_active = TRUE;
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'tax_rule_rate_check') THEN
        ALTER TABLE tax_rule ADD CONSTRAINT tax_rule_rate_check CHECK (rate >= 0 AND rate <= 1);
    END IF;
END $$;
