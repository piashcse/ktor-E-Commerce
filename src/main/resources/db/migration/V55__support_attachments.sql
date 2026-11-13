-- Images attached to support tickets.

CREATE TABLE IF NOT EXISTS support_attachment (
    id           VARCHAR(50)  PRIMARY KEY,
    created_at   TIMESTAMP    NOT NULL DEFAULT (NOW() AT TIME ZONE 'utc'),
    updated_at   TIMESTAMP,
    ticket_id    VARCHAR(50)  NOT NULL REFERENCES support_ticket(id) ON DELETE CASCADE,
    uploaded_by  VARCHAR(50)  NOT NULL REFERENCES "user"(id) ON DELETE CASCADE,
    file_name    VARCHAR(255) NOT NULL,
    content_type VARCHAR(100),
    storage_name VARCHAR(255) NOT NULL,
    size_bytes   BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT support_attachment_size_non_negative CHECK (size_bytes >= 0)
);

CREATE INDEX IF NOT EXISTS support_attachment_ticket_idx ON support_attachment(ticket_id);
