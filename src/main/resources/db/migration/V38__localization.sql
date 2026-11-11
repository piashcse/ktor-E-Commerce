-- Localization: product content translations and the checkout/order locale.
CREATE TABLE IF NOT EXISTS product_translation (
    id          VARCHAR(50) PRIMARY KEY,
    created_at  TIMESTAMP    NOT NULL DEFAULT (NOW() AT TIME ZONE 'utc'),
    updated_at  TIMESTAMP,
    product_id  VARCHAR(50)  NOT NULL REFERENCES product(id) ON DELETE CASCADE,
    locale      VARCHAR(10)  NOT NULL,
    name        TEXT         NOT NULL,
    description TEXT         NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS product_translation_product_locale_unique
  ON product_translation(product_id, locale);
CREATE INDEX IF NOT EXISTS product_translation_locale_idx ON product_translation(locale);

ALTER TABLE "order" ADD COLUMN IF NOT EXISTS locale VARCHAR(10) NOT NULL DEFAULT 'en';
