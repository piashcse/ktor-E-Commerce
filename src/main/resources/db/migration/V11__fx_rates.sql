-- V11: FX rates for multi-currency checkout (base USD snapshot per order in future).
CREATE TABLE IF NOT EXISTS fx_rate (
  id VARCHAR(50) PRIMARY KEY,
  created_at TIMESTAMP NOT NULL DEFAULT (NOW() AT TIME ZONE 'utc'),
  updated_at TIMESTAMP,
  base_currency VARCHAR(3) NOT NULL DEFAULT 'USD',
  target_currency VARCHAR(3) NOT NULL,
  rate DECIMAL(18,6) NOT NULL,
  effective_at TIMESTAMP NOT NULL DEFAULT (NOW() AT TIME ZONE 'utc')
);
CREATE UNIQUE INDEX IF NOT EXISTS fx_rate_base_target_idx ON fx_rate(base_currency, target_currency);
INSERT INTO fx_rate (id, base_currency, target_currency, rate)
VALUES ('fx-usd-usd', 'USD', 'USD', 1.0)
ON CONFLICT DO NOTHING;
