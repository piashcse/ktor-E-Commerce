# Transactional Outbox & Dead Letters

Domain events are written to `outbox` in the same transaction as the business change, then
relayed by `OutboxPoller` with consumer acknowledgement. Failures are retried and eventually
retired to `outbox_dead_letter` for inspection/replay.

## Customer-facing event types

`ORDER_PLACED`, `PAYMENT_COMPLETED`, `REFUND_STATUS_CHANGED`, `RMA_STATUS_CHANGED`,
`USER_REGISTERED`, `ADMIN_ACTION`, `SEND_EMAIL`.

## Admin endpoints

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/v1/admin/outbox/summary` | Pending rows + dead-letter count |
| `GET` | `/api/v1/admin/outbox/dead-letters` | Paginated dead letters |
| `POST` | `/api/v1/admin/outbox/dead-letters/{id}/requeue` | Requeue a payload |
| `POST` | `/api/v1/admin/outbox/dead-letters/requeue` | Bulk requeue dead letters |
| `GET` | `/api/v1/admin/outbox/dead-letters/{id}` | Inspect a dead letter |

## Semantics

- `published_at` is set only after all subscribers succeed (at-least-once delivery).
- Events are retried with exponential backoff and capped attempts.
- Poison rows are reported via metrics/logs; admin replay copies the payload back into the
  outbox and removes the dead letter.
- Notifications use templates and per-event channel preferences; email/SMS/push provider
  failures are retried and dead-lettered like any other subscriber.

See [Notifications](notification.md).
## Endpoint Details

### 1. Outbox summary

**`GET /api/v1/admin/outbox/summary`**

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/admin/outbox/summary' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <admin_token>'
```

#### Response

```json
{
  "pending": 1,
  "deadLetters": 1,
  "oldestPendingAt": "2024-05-06T12:00:00"
}
```

---

### 2. List dead letters

**`GET /api/v1/admin/outbox/dead-letters`**

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/admin/outbox/dead-letters?page=1&perPage=20' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <admin_token>'
```

#### Response

```json
{
  "data": [
    {
      "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "eventType": "CUSTOMER",
      "aggregateType": "CUSTOMER",
      "aggregateId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "payload": "{}",
      "error": "None",
      "attempts": 1,
      "createdAt": "2024-05-06T12:00:00"
    }
  ],
  "metadata": {
    "totalCount": 1,
    "limit": 20,
    "offset": 0,
    "page": 1,
    "perPage": 20,
    "totalPages": 1,
    "hasNext": false,
    "hasPrev": false
  }
}
```

---

### 3. Requeue dead letter

**`POST /api/v1/admin/outbox/dead-letters/{id}/requeue`**

```bash
curl -X 'POST' \
  'http://localhost:8080/api/v1/admin/outbox/dead-letters/6c5078d3-f8e3-4c88-9afe-48b5423c664f/requeue' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <admin_token>'
```

#### Response

```json
{
  "message": "Operation completed"
}
```

---

### 4. Bulk requeue dead letters

**`POST /api/v1/admin/outbox/dead-letters/requeue`**

```bash
curl -X 'POST' \
  'http://localhost:8080/api/v1/admin/outbox/dead-letters/requeue' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <admin_token>' \
  -H 'Content-Type: application/json' \
  -d '{
  "ids": [
    "6c5078d3-f8e3-4c88-9afe-48b5423c664f"
  ]
}'
```

#### Response

```json
{
  "message": "Operation completed"
}
```

---

### 5. Inspect a dead letter

**`GET /api/v1/admin/outbox/dead-letters/{id}`**

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/admin/outbox/dead-letters/6c5078d3-f8e3-4c88-9afe-48b5423c664f' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <admin_token>'
```

#### Response

```json
{
  "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "eventType": "CUSTOMER",
  "aggregateType": "CUSTOMER",
  "aggregateId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "payload": "{}",
  "error": "None",
  "attempts": 1,
  "createdAt": "2024-05-06T12:00:00"
}
```

---
