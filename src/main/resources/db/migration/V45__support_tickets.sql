-- Customer support tickets and threaded messages.
CREATE TABLE IF NOT EXISTS support_ticket (
    id               VARCHAR(50) PRIMARY KEY,
    created_at       TIMESTAMP    NOT NULL DEFAULT (NOW() AT TIME ZONE 'utc'),
    updated_at       TIMESTAMP,
    user_id          VARCHAR(50)  NOT NULL REFERENCES "user"(id) ON DELETE CASCADE,
    order_id         VARCHAR(50)  REFERENCES "order"(id) ON DELETE SET NULL,
    shop_id          VARCHAR(50)  REFERENCES shop(id) ON DELETE SET NULL,
    subject          VARCHAR(200) NOT NULL,
    category         VARCHAR(30)  NOT NULL DEFAULT 'OTHER',
    priority         VARCHAR(20)  NOT NULL DEFAULT 'NORMAL',
    status           VARCHAR(20)  NOT NULL DEFAULT 'OPEN',
    assigned_to      VARCHAR(50)  REFERENCES "user"(id) ON DELETE SET NULL,
    last_message_at  TIMESTAMP,
    resolved_at      TIMESTAMP
);

CREATE INDEX IF NOT EXISTS support_ticket_user_idx ON support_ticket(user_id, created_at);
CREATE INDEX IF NOT EXISTS support_ticket_status_idx ON support_ticket(status, priority);
CREATE INDEX IF NOT EXISTS support_ticket_assigned_idx ON support_ticket(assigned_to);

CREATE TABLE IF NOT EXISTS support_message (
    id          VARCHAR(50) PRIMARY KEY,
    created_at  TIMESTAMP   NOT NULL DEFAULT (NOW() AT TIME ZONE 'utc'),
    updated_at  TIMESTAMP,
    ticket_id   VARCHAR(50) NOT NULL REFERENCES support_ticket(id) ON DELETE CASCADE,
    sender_id   VARCHAR(50) NOT NULL REFERENCES "user"(id) ON DELETE CASCADE,
    sender_role VARCHAR(20) NOT NULL,
    body        TEXT        NOT NULL,
    is_internal BOOLEAN     NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS support_message_ticket_idx ON support_message(ticket_id, created_at);
