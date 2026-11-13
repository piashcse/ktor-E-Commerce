ALTER TABLE policy_consents
    ADD COLUMN IF NOT EXISTS policy_version VARCHAR(50),
    ADD COLUMN IF NOT EXISTS policy_content_hash VARCHAR(64);
