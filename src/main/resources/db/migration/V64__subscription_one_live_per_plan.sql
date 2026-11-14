-- Prevent duplicate concurrent subscribe requests from creating more than one
-- live subscription (and invoice) for the same customer and plan.
CREATE UNIQUE INDEX IF NOT EXISTS subscription_one_live_per_plan
    ON subscription(user_id, plan_id)
    WHERE status IN ('PENDING', 'ACTIVE', 'PAST_DUE');
