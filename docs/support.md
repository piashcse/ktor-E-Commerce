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
| `POST` | `/api/v1/support/tickets/{id}/satisfaction` | Rate a resolved/closed ticket |
| `POST` | `/api/v1/support/tickets/{id}/attachments` | Upload an image attachment |
| `GET` | `/api/v1/support/tickets/{id}/attachments/{attachmentId}` | Download an attachment |

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

SLA targets by priority:

| Priority | First response | Resolution |
|---|---|---|
| `URGENT` | 1 hour | 4 hours |
| `HIGH` | 4 hours | 24 hours |
| `NORMAL` | 24 hours | 72 hours |
| `LOW` | 48 hours | 120 hours |

Ticket responses include `firstResponseAt`, `responseDueAt`, `resolutionDueAt` and `slaBreached`.
The first admin reply records `firstResponseAt`.

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
    "createdAt": "2026-05-01T10:00:00Z",
    "firstResponseAt": null,
    "responseDueAt": "2026-05-02T10:00:00Z",
    "resolutionDueAt": "2026-05-04T10:00:00Z",
    "slaBreached": false,
    "csatScore": null,
    "csatComment": null,
    "csatRatedAt": null
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


## Attachments

`POST /api/v1/support/tickets/{id}/attachments` accepts a multipart image (JPEG/PNG/WebP/GIF, 5 MB)
from the ticket owner. `GET /api/v1/support/tickets/{id}/attachments/{attachmentId}` and the admin
counterpart serve the file privately with `Cache-Control: private, no-store`.

Attachments appear in ticket responses:

```json
{
  "attachments": [
    {
      "id": "attachment-uuid",
      "ticketId": "ticket-uuid",
      "fileName": "proof.png",
      "contentType": "image/png",
      "sizeBytes": 12345,
      "createdAt": "2026-05-01T10:00:00Z"
    }
  ]
}
```

## Satisfaction (CSAT)

**`POST /api/v1/support/tickets/{id}/satisfaction`**

```json
{ "score": 5, "comment": "Fast resolution" }
```

- Score must be between 1 and 5.
- Only the ticket owner can rate, and only after the ticket is `RESOLVED` or `CLOSED`.
- Re-rating replaces the previous score/comment.
