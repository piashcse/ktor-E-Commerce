-- Lifecycle consistency: gift-card redemption reversal audit.
ALTER TABLE gift_card_redemption ADD COLUMN IF NOT EXISTS reversed_at TIMESTAMP;
