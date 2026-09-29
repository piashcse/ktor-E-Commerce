-- V14: transactional outbox for durable domain events (money paths first).
CREATE TABLE IF NOT EXISTS outbox (
  id VARCHAR(50) PRIMARY KEY,
  created_at TIMESTAMP NOT NULL DEFAULT (NOW() AT TIME ZONE 'utc'),
  updated_at TIMESTAMP,
  aggregate_type VARCHAR(50) NOT NULL,
  aggregate_id VARCHAR(50) NOT NULL,
  event_type VARCHAR(50) NOT NULL,
  payload TEXT NOT NULL,
  published_at TIMESTAMP NULL,
  attempts INT NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS outbox_unpublished_idx ON outbox(published_at, created_at);
CREATE INDEX IF NOT EXISTS outbox_event_type_idx ON outbox(event_type);
