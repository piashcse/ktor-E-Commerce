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

## Semantics

- `published_at` is set only after all subscribers succeed (at-least-once delivery).
- Events are retried with exponential backoff and capped attempts.
- Poison rows are reported via metrics/logs; admin replay copies the payload back into the
  outbox and removes the dead letter.
- Notifications use templates and per-event channel preferences; email/SMS/push provider
  failures are retried and dead-lettered like any other subscriber.

See [Notifications](notification.md).
