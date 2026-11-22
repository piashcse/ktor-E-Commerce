-- OTPs are stored as SHA-256 hex digests (64 chars). The original VARCHAR(6)
-- columns could only hold the pre-hashing six-digit value, so every
-- registration/password-reset write failed with SQLSTATE 22001.
ALTER TABLE "user" ALTER COLUMN otp_code TYPE VARCHAR(64);
ALTER TABLE "user" ALTER COLUMN reset_otp_code TYPE VARCHAR(64);
