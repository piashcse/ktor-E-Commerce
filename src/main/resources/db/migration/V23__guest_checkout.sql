-- V23: guest checkout support.
ALTER TABLE "user" ADD COLUMN IF NOT EXISTS is_guest BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE "order" ADD COLUMN IF NOT EXISTS contact_email VARCHAR(255);
CREATE INDEX IF NOT EXISTS order_contact_email_idx ON "order"(contact_email);
CREATE INDEX IF NOT EXISTS order_number_email_idx ON "order"(order_number, contact_email);
