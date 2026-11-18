-- Admin/seller UX: decision reasons, review replies, dispute threads and
-- shop-scoped shipping methods.

ALTER TABLE shop ADD COLUMN IF NOT EXISTS rejection_reason TEXT;
ALTER TABLE shop ADD COLUMN IF NOT EXISTS suspended_reason TEXT;

ALTER TABLE seller_payout ADD COLUMN IF NOT EXISTS rejection_reason TEXT;
ALTER TABLE payout_request ADD COLUMN IF NOT EXISTS rejection_reason TEXT;
ALTER TABLE refund_request ADD COLUMN IF NOT EXISTS decision_reason TEXT;
ALTER TABLE rma ADD COLUMN IF NOT EXISTS resolution_note TEXT;

ALTER TABLE review_rating ADD COLUMN IF NOT EXISTS seller_reply TEXT;
ALTER TABLE review_rating ADD COLUMN IF NOT EXISTS seller_replied_at TIMESTAMP;
ALTER TABLE review_rating ADD COLUMN IF NOT EXISTS moderation_reason TEXT;

ALTER TABLE seller_review ADD COLUMN IF NOT EXISTS seller_reply TEXT;
ALTER TABLE seller_review ADD COLUMN IF NOT EXISTS seller_replied_at TIMESTAMP;
ALTER TABLE seller_review ADD COLUMN IF NOT EXISTS moderation_reason TEXT;

ALTER TABLE shipping_method ADD COLUMN IF NOT EXISTS shop_id VARCHAR(50) REFERENCES shop(id) ON DELETE CASCADE;
CREATE INDEX IF NOT EXISTS shipping_method_shop_idx ON shipping_method(shop_id);

CREATE TABLE IF NOT EXISTS dispute_message (
    id VARCHAR(50) PRIMARY KEY,
    dispute_id VARCHAR(50) NOT NULL REFERENCES seller_dispute(id) ON DELETE CASCADE,
    author_id VARCHAR(50) NOT NULL REFERENCES "user"(id) ON DELETE CASCADE,
    body TEXT NOT NULL,
    is_internal BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT (now() AT TIME ZONE 'utc'),
    updated_at TIMESTAMP
);
CREATE INDEX IF NOT EXISTS dispute_message_dispute_idx ON dispute_message(dispute_id, created_at);

CREATE TABLE IF NOT EXISTS kyc_status_history (
    id VARCHAR(50) PRIMARY KEY,
    seller_id VARCHAR(50) NOT NULL REFERENCES seller(id) ON DELETE CASCADE,
    status VARCHAR(20) NOT NULL,
    reason TEXT,
    actor_id VARCHAR(50) REFERENCES "user"(id) ON DELETE SET NULL,
    created_at TIMESTAMP NOT NULL DEFAULT (now() AT TIME ZONE 'utc'),
    updated_at TIMESTAMP
);
CREATE INDEX IF NOT EXISTS kyc_status_history_seller_idx ON kyc_status_history(seller_id, created_at);

ALTER TABLE chargeback ADD COLUMN IF NOT EXISTS evidence_url TEXT;
CREATE INDEX IF NOT EXISTS chargeback_evidence_due_idx ON chargeback(evidence_due_at);

ALTER TABLE promotion ADD COLUMN IF NOT EXISTS shop_id VARCHAR(50) REFERENCES shop(id) ON DELETE CASCADE;
CREATE INDEX IF NOT EXISTS promotion_shop_idx ON promotion(shop_id);
