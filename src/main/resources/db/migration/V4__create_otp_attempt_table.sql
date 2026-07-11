-- V4: Create OTP attempt tracking table for persistent rate limiting
CREATE TABLE IF NOT EXISTS otp_attempt (
    id              VARCHAR(50) PRIMARY KEY,
    created_at      TIMESTAMP NOT NULL DEFAULT (NOW() AT TIME ZONE 'utc'),
    updated_at      TIMESTAMP,
    user_id         VARCHAR(50) NOT NULL REFERENCES "user"(id),
    attempt_count   INTEGER NOT NULL DEFAULT 0,
    locked_until    TIMESTAMP
);

CREATE UNIQUE INDEX otp_attempt_user_id_idx ON otp_attempt(user_id);
