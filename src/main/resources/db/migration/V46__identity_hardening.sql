-- Social login linking and WebAuthn passkeys.
ALTER TABLE "user" ADD COLUMN IF NOT EXISTS social_provider VARCHAR(30);
ALTER TABLE "user" ADD COLUMN IF NOT EXISTS social_subject VARCHAR(255);

CREATE UNIQUE INDEX IF NOT EXISTS user_social_identity_unique
  ON "user"(social_provider, social_subject)
  WHERE social_provider IS NOT NULL AND social_subject IS NOT NULL;

CREATE TABLE IF NOT EXISTS passkey_credential (
    id            VARCHAR(50) PRIMARY KEY,
    created_at    TIMESTAMP    NOT NULL DEFAULT (NOW() AT TIME ZONE 'utc'),
    updated_at    TIMESTAMP,
    user_id       VARCHAR(50)  NOT NULL REFERENCES "user"(id) ON DELETE CASCADE,
    credential_id VARCHAR(500) NOT NULL,
    public_key    TEXT         NOT NULL,
    sign_count    BIGINT       NOT NULL DEFAULT 0,
    transports    VARCHAR(200),
    name          VARCHAR(100),
    last_used_at  TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS passkey_credential_id_unique ON passkey_credential(credential_id);
CREATE INDEX IF NOT EXISTS passkey_credential_user_idx ON passkey_credential(user_id);
