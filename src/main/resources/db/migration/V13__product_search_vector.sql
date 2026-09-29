-- V13: full-text search vector for products (name + description), kept in sync by trigger.
ALTER TABLE product ADD COLUMN IF NOT EXISTS search_vector tsvector;
CREATE INDEX IF NOT EXISTS product_search_vector_gin ON product USING GIN (search_vector);
CREATE OR REPLACE FUNCTION product_search_vector_trigger() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
  NEW.search_vector := to_tsvector('english', COALESCE(NEW.name, '') || ' ' || COALESCE(NEW.description, ''));
  RETURN NEW;
END $$;
DROP TRIGGER IF EXISTS product_search_vector_update ON product;
CREATE TRIGGER product_search_vector_update
  BEFORE INSERT OR UPDATE OF name, description ON product
  FOR EACH ROW EXECUTE FUNCTION product_search_vector_trigger();
-- Backfill existing rows.
UPDATE product SET search_vector = to_tsvector('english', COALESCE(name, '') || ' ' || COALESCE(description, ''))
WHERE search_vector IS NULL;
