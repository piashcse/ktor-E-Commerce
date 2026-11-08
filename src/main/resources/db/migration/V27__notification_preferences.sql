-- V27: per-event notification channel preferences.
CREATE TABLE IF NOT EXISTS notification_preference (
    id          VARCHAR(50) PRIMARY KEY,
    created_at  TIMESTAMP NOT NULL DEFAULT (NOW() AT TIME ZONE 'utc'),
    updated_at  TIMESTAMP,
    user_id     VARCHAR(50) NOT NULL REFERENCES "user"(id) ON DELETE CASCADE,
    event_type  VARCHAR(50) NOT NULL,
    channel     VARCHAR(20) NOT NULL
);
CREATE UNIQUE INDEX IF NOT EXISTS notification_preference_user_event_unique
  ON notification_preference(user_id, event_type);
CREATE INDEX IF NOT EXISTS notification_preference_user_idx ON notification_preference(user_id);
