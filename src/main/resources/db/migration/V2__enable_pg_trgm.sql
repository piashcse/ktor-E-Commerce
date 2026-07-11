-- V2: Enable pg_trgm extension for fuzzy search and create GIN indexes

CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX IF NOT EXISTS product_name_trgm_idx ON product USING GIN (name gin_trgm_ops);

CREATE INDEX IF NOT EXISTS product_description_trgm_idx ON product USING GIN (description gin_trgm_ops);
