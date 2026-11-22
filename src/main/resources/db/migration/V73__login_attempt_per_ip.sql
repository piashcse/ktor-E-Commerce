-- Lockouts must not let an attacker from one IP lock out the legitimate
-- account owner on another IP. Track attempts per (email, role, IP).
DROP INDEX IF EXISTS login_attempt_email_user_type_idx;
CREATE UNIQUE INDEX IF NOT EXISTS login_attempt_email_type_ip_idx
  ON login_attempt (email, user_type, COALESCE(ip_address, ''));
