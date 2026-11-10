-- V26: seller KYC lifecycle and statement support.
ALTER TABLE seller ADD COLUMN IF NOT EXISTS kyc_status VARCHAR(20) NOT NULL DEFAULT 'NOT_SUBMITTED';
ALTER TABLE seller ADD COLUMN IF NOT EXISTS kyc_document_url VARCHAR(500);
ALTER TABLE seller ADD COLUMN IF NOT EXISTS kyc_submitted_at TIMESTAMP;
ALTER TABLE seller ADD COLUMN IF NOT EXISTS kyc_verified_at TIMESTAMP;
ALTER TABLE seller ADD COLUMN IF NOT EXISTS kyc_rejection_reason TEXT;
CREATE INDEX IF NOT EXISTS seller_kyc_status_idx ON seller(kyc_status);

-- Grandfather already-approved sellers so existing payouts keep working.
UPDATE seller SET kyc_status = 'VERIFIED', kyc_verified_at = NOW()
WHERE status = 'APPROVED' AND kyc_status = 'NOT_SUBMITTED';
