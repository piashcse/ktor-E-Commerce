-- Support SLA tracking and customer satisfaction scores.

ALTER TABLE support_ticket ADD COLUMN IF NOT EXISTS first_response_at TIMESTAMP;
ALTER TABLE support_ticket ADD COLUMN IF NOT EXISTS csat_score INTEGER;
ALTER TABLE support_ticket ADD COLUMN IF NOT EXISTS csat_comment TEXT;
ALTER TABLE support_ticket ADD COLUMN IF NOT EXISTS csat_rated_at TIMESTAMP;

CREATE INDEX IF NOT EXISTS support_ticket_first_response_idx ON support_ticket(first_response_at);
CREATE INDEX IF NOT EXISTS support_ticket_csat_idx ON support_ticket(csat_score);
