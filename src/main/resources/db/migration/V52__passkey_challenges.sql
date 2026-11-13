-- Shared WebAuthn challenge store so multi-node deployments can finish a ceremony
-- started on another instance.

CREATE TABLE IF NOT EXISTS passkey_challenge (
    id          VARCHAR(50) PRIMARY KEY,
    created_at  TIMESTAMP   NOT NULL DEFAULT (NOW() AT TIME ZONE 'utc'),
    updated_at  TIMESTAMP,
    challenge   VARCHAR(200) NOT NULL,
    user_id     VARCHAR(50) NOT NULL REFERENCES "user"(id) ON DELETE CASCADE,
    type        VARCHAR(20) NOT NULL,
    expires_at  TIMESTAMP   NOT NULL,
    consumed_at TIMESTAMP,
    CONSTRAINT passkey_challenge_type_valid CHECK (type IN ('register', 'login'))
);

CREATE UNIQUE INDEX IF NOT EXISTS passkey_challenge_value_unique ON passkey_challenge(challenge);
CREATE INDEX IF NOT EXISTS passkey_challenge_expiry_idx ON passkey_challenge(expires_at);
