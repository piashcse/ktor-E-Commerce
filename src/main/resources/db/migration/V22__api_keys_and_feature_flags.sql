-- V22: API keys, permission scopes and feature flags.
CREATE TABLE IF NOT EXISTS api_key (
    id           VARCHAR(50) PRIMARY KEY,
    created_at   TIMESTAMP NOT NULL DEFAULT (NOW() AT TIME ZONE 'utc'),
    updated_at   TIMESTAMP,
    user_id      VARCHAR(50) NOT NULL REFERENCES "user"(id) ON DELETE CASCADE,
    name         VARCHAR(100) NOT NULL,
    key_hash     VARCHAR(64) NOT NULL,
    prefix       VARCHAR(16) NOT NULL,
    scopes       TEXT NOT NULL,
    is_active    BOOLEAN NOT NULL DEFAULT TRUE,
    last_used_at TIMESTAMP,
    expires_at   TIMESTAMP
);
CREATE UNIQUE INDEX IF NOT EXISTS api_key_hash_unique ON api_key(key_hash);
CREATE INDEX IF NOT EXISTS api_key_user_id_idx ON api_key(user_id);

CREATE TABLE IF NOT EXISTS feature_flag (
    id          VARCHAR(50) PRIMARY KEY,
    created_at  TIMESTAMP NOT NULL DEFAULT (NOW() AT TIME ZONE 'utc'),
    updated_at  TIMESTAMP,
    key         VARCHAR(100) NOT NULL,
    enabled     BOOLEAN NOT NULL DEFAULT FALSE,
    description TEXT
);
CREATE UNIQUE INDEX IF NOT EXISTS feature_flag_key_unique ON feature_flag(key);

INSERT INTO feature_flag (id, key, enabled, description)
VALUES
    ('ff-guest-checkout', 'guest_checkout', FALSE, 'Allow unauthenticated guest checkout'),
    ('ff-require-policy-consent', 'require_policy_consent', FALSE, 'Require policy consent before checkout'),
    ('ff-tax-rules', 'tax_rules', TRUE, 'Use configurable tax rules instead of the default rate')
ON CONFLICT (key) DO NOTHING;
