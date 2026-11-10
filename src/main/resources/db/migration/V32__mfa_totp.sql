-- TOTP multi-factor authentication.
ALTER TABLE "user" ADD COLUMN mfa_secret VARCHAR(64);
ALTER TABLE "user" ADD COLUMN mfa_enabled BOOLEAN NOT NULL DEFAULT FALSE;
