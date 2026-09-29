-- V12: separate OTP attempt counters per purpose (registration vs password-reset were clobbering each other).
ALTER TABLE otp_attempt ADD COLUMN IF NOT EXISTS purpose VARCHAR(20) NOT NULL DEFAULT 'GENERAL';
-- Backfill: existing rows stay GENERAL; reset-flow rows will be created as RESET going forward.
DO $$ BEGIN
  IF EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'otp_attempt_user_id_unique') THEN
    ALTER TABLE otp_attempt DROP CONSTRAINT otp_attempt_user_id_unique;
  END IF;
END $$;
-- Drop old unique index on user_id if present (auto-named).
DROP INDEX IF EXISTS otp_attempt_user_id_unique;
CREATE UNIQUE INDEX IF NOT EXISTS otp_attempt_user_purpose_unique ON otp_attempt(user_id, purpose);
