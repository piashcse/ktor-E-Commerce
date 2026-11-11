# Support Tickets

Customer support with threaded replies, internal notes, assignment, priorities and resolution.

**Base URL:** `http://localhost:8080`

## Customer flow

**`POST /api/v1/support/tickets`**

```json
{
  "subject": "Missing item",
  "body": "One item was missing from the parcel",
  "category": "ORDER",
  "priority": "NORMAL",
  "orderId": "order-uuid"
}
```

- `orderId` is optional; when supplied it must belong to the caller and is linked to the ticket.
- The first message is recorded as a `CUSTOMER` message.
- New tickets start `OPEN`.

Categories: `ORDER`, `PAYMENT`, `ACCOUNT`, `PRODUCT`, `OTHER`.

Other customer endpoints:

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/v1/support/tickets` | My tickets |
| `GET` | `/api/v1/support/tickets/{id}` | Ticket + public replies |
| `POST` | `/api/v1/support/tickets/{id}/messages` | Reply |
| `POST` | `/api/v1/support/tickets/{id}/close` | Close |

- Customers never see `isInternal` messages.
- Replying reopens `PENDING` / `RESOLVED` tickets.
- Closed tickets cannot receive customer replies.

## Admin queue

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/v1/admin/support/tickets?status=&priority=&assignedTo=` | Filtered list |
| `GET` | `/api/v1/admin/support/tickets/{id}` | Full thread incl. internal notes |
| `POST` | `/api/v1/admin/support/tickets/{id}/messages` | Reply or internal note |
| `PUT` | `/api/v1/admin/support/tickets/{id}` | Status/priority/category/assignment |

Reply body:

```json
{ "body": "Checking with the carrier", "isInternal": true }
```

Update body:

```json
{
  "status": "RESOLVED",
  "priority": "HIGH",
  "category": "ORDER",
  "assignedTo": "admin-uuid"
}
```

Send a blank `assignedTo` to unassign.

Statuses: `OPEN`, `PENDING`, `RESOLVED`, `CLOSED`.

- `RESOLVED` / `CLOSED` record `resolvedAt`.
- `OPEN` clears `resolvedAt`.
- Every reply updates `lastMessageAt`, which drives the queue ordering.

## Ticket response

```json
{
  "ticket": {
    "id": "ticket-uuid",
    "userId": "user-uuid",
    "orderId": "order-uuid",
    "shopId": "shop-uuid",
    "subject": "Missing item",
    "category": "ORDER",
    "priority": "NORMAL",
    "status": "OPEN",
    "assignedTo": null,
    "lastMessageAt": "2026-05-01T10:00:00Z",
    "resolvedAt": null,
    "createdAt": "2026-05-01T10:00:00Z"
  },
  "messages": [
    {
      "id": "message-uuid",
      "ticketId": "ticket-uuid",
      "senderId": "user-uuid",
      "senderRole": "CUSTOMER",
      "body": "One item was missing from the parcel",
      "isInternal": false,
      "createdAt": "2026-05-01T10:00:00Z"
    }
  ]
}
```

This is polling-based chat: clients fetch the ticket thread and post replies. No WebSocket channel is
required.
