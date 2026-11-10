# Notifications

Notifications are rendered from centralized templates and delivered through a channel dispatcher.

## Channels

| Channel | Behavior |
|---|---|
| `IN_APP` | Always written to the user's notification inbox |
| `EMAIL` | Sent through SMTP (default extra channel) |
| `SMS` | Inert until an SMS provider is configured; skipped events are logged |
| `PUSH` | Inert until a push provider is configured; skipped events are logged |

Domain events currently dispatched: `ORDER_PLACED`, `PAYMENT_COMPLETED`,
`REFUND_<STATUS>`, `RMA_<STATUS>`, `USER_REGISTERED`.

## Global channel preference

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/v1/notifications/preferences` | Current default channel |
| `PUT` | `/api/v1/notifications/preferences` | Set default channel |

```json
{ "channel": "EMAIL" }
```

## Per-event preference

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/v1/notifications/preferences/{eventType}` | Resolve channel for an event |
| `PUT` | `/api/v1/notifications/preferences/{eventType}` | Override channel for an event |

Example: `PUT /api/v1/notifications/preferences/REFUND_APPROVED` with
`{ "channel": "SMS" }`. The in-app notification is always recorded; the
override controls the additional delivery channel.

## Inbox

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/v1/notifications` | Paginated inbox |
| `GET` | `/api/v1/notifications/unread-count` | Unread count |
| `GET` | `/api/v1/notifications/cursor` | Keyset-paginated inbox |
| `POST` | `/api/v1/notifications/{id}/read` | Mark one read |
| `POST` | `/api/v1/notifications/read-all` | Mark all read |

Sellers/admins can send announcements with `POST /api/v1/notifications/send`
(sellers may only target customers who ordered from their shop).
