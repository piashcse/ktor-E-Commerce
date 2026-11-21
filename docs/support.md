# Support Tickets

Support tickets with replies, attachments, SLAs and CSAT.

### 1. Customer flow

**`POST /api/v1/support/tickets`**
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
| `GET` | `/api/v1/admin/support/tickets/{id}/attachments/{attachmentId}` | Admin: download a support attachment |

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

`POST /api/v1/support/tickets/{id}/attachments` accepts a single multipart image from the ticket
owner. Send one file part (`image/jpeg`, `image/png` or `image/webp`; maximum **5 MB**). The field
name is not enforced; the examples use `file`. `GET /api/v1/support/tickets/{id}/attachments/{attachmentId}`
and the admin counterpart serve the file privately with `Cache-Control: private, no-store`.

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

### 2. Satisfaction (CSAT)

**`POST /api/v1/support/tickets/{id}/satisfaction`**
{ "score": 5, "comment": "Fast resolution" }
```

- Score must be between 1 and 5.
- Only the ticket owner can rate, and only after the ticket is `RESOLVED` or `CLOSED`.
- Re-rating replaces the previous score/comment.
## Endpoint Details

### 3. List my support tickets

**`GET /api/v1/support/tickets`**

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/support/tickets?page=1&perPage=20' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <access_token>'
```

#### Response

```json
{
  "data": [
    {
      "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "userId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "orderId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "shopId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "subject": "Looks good",
      "category": "ORDER",
      "priority": "LOW",
      "status": "PENDING",
      "assignedTo": "string",
      "lastMessageAt": "2024-05-06T12:00:00",
      "resolvedAt": "2024-05-06T12:00:00",
      "createdAt": "2024-05-06T12:00:00",
      "firstResponseAt": "2024-05-06T12:00:00",
      "responseDueAt": "2024-05-06T12:00:00",
      "resolutionDueAt": "2024-05-06T12:00:00",
      "slaBreached": true,
      "csatScore": 1,
      "csatComment": "string",
      "csatRatedAt": "250.00"
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

### 4. Get a support ticket and public replies

**`GET /api/v1/support/tickets/{id}`**

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/support/tickets/6c5078d3-f8e3-4c88-9afe-48b5423c664f' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <access_token>'
```

#### Response

```json
{
  "ticket": {
    "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
    "userId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
    "orderId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
    "shopId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
    "subject": "Looks good",
    "category": "ORDER",
    "priority": "LOW",
    "status": "PENDING",
    "assignedTo": "string",
    "lastMessageAt": "2024-05-06T12:00:00",
    "resolvedAt": "2024-05-06T12:00:00",
    "createdAt": "2024-05-06T12:00:00",
    "firstResponseAt": "2024-05-06T12:00:00",
    "responseDueAt": "2024-05-06T12:00:00",
    "resolutionDueAt": "2024-05-06T12:00:00",
    "slaBreached": true,
    "csatScore": 1,
    "csatComment": "string",
    "csatRatedAt": "250.00"
  },
  "messages": [
    {
      "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "ticketId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "senderId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "senderRole": "CUSTOMER",
      "body": "Looks good",
      "isInternal": true,
      "createdAt": "2024-05-06T12:00:00"
    }
  ],
  "attachments": [
    {
      "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "ticketId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "fileName": "string",
      "contentType": "CUSTOMER",
      "sizeBytes": 1,
      "createdAt": "2024-05-06T12:00:00"
    }
  ]
}
```

---

### 5. Reply to a support ticket

**`POST /api/v1/support/tickets/{id}/messages`**

```bash
curl -X 'POST' \
  'http://localhost:8080/api/v1/support/tickets/6c5078d3-f8e3-4c88-9afe-48b5423c664f/messages' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <access_token>' \
  -H 'Content-Type: application/json' \
  -d '{
  "body": "Looks good",
  "isInternal": true
}'
```

#### Response

```json
{
  "ticket": {
    "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
    "userId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
    "orderId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
    "shopId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
    "subject": "Looks good",
    "category": "ORDER",
    "priority": "LOW",
    "status": "PENDING",
    "assignedTo": "string",
    "lastMessageAt": "2024-05-06T12:00:00",
    "resolvedAt": "2024-05-06T12:00:00",
    "createdAt": "2024-05-06T12:00:00",
    "firstResponseAt": "2024-05-06T12:00:00",
    "responseDueAt": "2024-05-06T12:00:00",
    "resolutionDueAt": "2024-05-06T12:00:00",
    "slaBreached": true,
    "csatScore": 1,
    "csatComment": "string",
    "csatRatedAt": "250.00"
  },
  "messages": [
    {
      "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "ticketId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "senderId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "senderRole": "CUSTOMER",
      "body": "Looks good",
      "isInternal": true,
      "createdAt": "2024-05-06T12:00:00"
    }
  ],
  "attachments": [
    {
      "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "ticketId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "fileName": "string",
      "contentType": "CUSTOMER",
      "sizeBytes": 1,
      "createdAt": "2024-05-06T12:00:00"
    }
  ]
}
```

---

### 6. Close my support ticket

**`POST /api/v1/support/tickets/{id}/close`**

```bash
curl -X 'POST' \
  'http://localhost:8080/api/v1/support/tickets/6c5078d3-f8e3-4c88-9afe-48b5423c664f/close' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <access_token>'
```

#### Response

```json
{
  "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "userId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "orderId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "shopId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "subject": "Looks good",
  "category": "ORDER",
  "priority": "LOW",
  "status": "PENDING",
  "assignedTo": "string",
  "lastMessageAt": "2024-05-06T12:00:00",
  "resolvedAt": "2024-05-06T12:00:00",
  "createdAt": "2024-05-06T12:00:00",
  "firstResponseAt": "2024-05-06T12:00:00",
  "responseDueAt": "2024-05-06T12:00:00",
  "resolutionDueAt": "2024-05-06T12:00:00",
  "slaBreached": true,
  "csatScore": 1,
  "csatComment": "string",
  "csatRatedAt": "250.00"
}
```

---

### 7. Attach an image to my support ticket

**`POST /api/v1/support/tickets/{id}/attachments`**

#### Multipart Form Data

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `file` | file | Yes | `image/jpeg`, `image/png` or `image/webp` (`.jpg`, `.jpeg`, `.png`, `.webp`). Maximum **5 MB**. |

#### Headers

| Header | Value | Required |
|--------|-------|----------|
| `Authorization` | `Bearer <access_token>` | Yes |
| `Content-Type` | `multipart/form-data` | Yes |

#### Request

```bash
curl -X 'POST' \
  'http://localhost:8080/api/v1/support/tickets/{id}/attachments' \
  -H 'Authorization: Bearer <access_token>' \
  -H 'Content-Type: multipart/form-data' \
  -F 'file=@attachment.png;type=image/png'
```

#### Response

```json
{
  "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "ticketId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "fileName": "string",
  "contentType": "CUSTOMER",
  "sizeBytes": 1,
  "createdAt": "2024-05-06T12:00:00"
}
```

---

### 8. Download my support attachment

**`GET /api/v1/support/tickets/{id}/attachments/{attachmentId}`**

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/support/tickets/6c5078d3-f8e3-4c88-9afe-48b5423c664f/attachments/6c5078d3-f8e3-4c88-9afe-48b5423c664f' \
  -H 'accept: */*' \
  -H 'Authorization: Bearer <access_token>'
```

#### Response

```text
<binary attachment content>
```

---
### 9. Admin: list support tickets

**`GET /api/v1/admin/support/tickets`**

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/admin/support/tickets?page=1&perPage=20&status=PENDING' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <admin_token>'
```

#### Response

```json
{
  "data": [
    {
      "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "userId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "orderId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "shopId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "subject": "Looks good",
      "category": "ORDER",
      "priority": "LOW",
      "status": "PENDING",
      "assignedTo": "string",
      "lastMessageAt": "2024-05-06T12:00:00",
      "resolvedAt": "2024-05-06T12:00:00",
      "createdAt": "2024-05-06T12:00:00",
      "firstResponseAt": "2024-05-06T12:00:00",
      "responseDueAt": "2024-05-06T12:00:00",
      "resolutionDueAt": "2024-05-06T12:00:00",
      "slaBreached": true,
      "csatScore": 1,
      "csatComment": "string",
      "csatRatedAt": "250.00"
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

### 10. Admin: get a ticket with internal notes

**`GET /api/v1/admin/support/tickets/{id}`**

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/admin/support/tickets/6c5078d3-f8e3-4c88-9afe-48b5423c664f' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <admin_token>'
```

#### Response

```json
{
  "ticket": {
    "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
    "userId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
    "orderId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
    "shopId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
    "subject": "Looks good",
    "category": "ORDER",
    "priority": "LOW",
    "status": "PENDING",
    "assignedTo": "string",
    "lastMessageAt": "2024-05-06T12:00:00",
    "resolvedAt": "2024-05-06T12:00:00",
    "createdAt": "2024-05-06T12:00:00",
    "firstResponseAt": "2024-05-06T12:00:00",
    "responseDueAt": "2024-05-06T12:00:00",
    "resolutionDueAt": "2024-05-06T12:00:00",
    "slaBreached": true,
    "csatScore": 1,
    "csatComment": "string",
    "csatRatedAt": "250.00"
  },
  "messages": [
    {
      "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "ticketId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "senderId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "senderRole": "CUSTOMER",
      "body": "Looks good",
      "isInternal": true,
      "createdAt": "2024-05-06T12:00:00"
    }
  ],
  "attachments": [
    {
      "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "ticketId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "fileName": "string",
      "contentType": "CUSTOMER",
      "sizeBytes": 1,
      "createdAt": "2024-05-06T12:00:00"
    }
  ]
}
```

---

### 11. Admin: reply or add an internal note

**`POST /api/v1/admin/support/tickets/{id}/messages`**

```bash
curl -X 'POST' \
  'http://localhost:8080/api/v1/admin/support/tickets/6c5078d3-f8e3-4c88-9afe-48b5423c664f/messages' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <admin_token>' \
  -H 'Content-Type: application/json' \
  -d '{
  "body": "Looks good",
  "isInternal": true
}'
```

#### Response

```json
{
  "ticket": {
    "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
    "userId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
    "orderId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
    "shopId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
    "subject": "Looks good",
    "category": "ORDER",
    "priority": "LOW",
    "status": "PENDING",
    "assignedTo": "string",
    "lastMessageAt": "2024-05-06T12:00:00",
    "resolvedAt": "2024-05-06T12:00:00",
    "createdAt": "2024-05-06T12:00:00",
    "firstResponseAt": "2024-05-06T12:00:00",
    "responseDueAt": "2024-05-06T12:00:00",
    "resolutionDueAt": "2024-05-06T12:00:00",
    "slaBreached": true,
    "csatScore": 1,
    "csatComment": "string",
    "csatRatedAt": "250.00"
  },
  "messages": [
    {
      "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "ticketId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "senderId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "senderRole": "CUSTOMER",
      "body": "Looks good",
      "isInternal": true,
      "createdAt": "2024-05-06T12:00:00"
    }
  ],
  "attachments": [
    {
      "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "ticketId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "fileName": "string",
      "contentType": "CUSTOMER",
      "sizeBytes": 1,
      "createdAt": "2024-05-06T12:00:00"
    }
  ]
}
```

---

### 12. Admin: update ticket status/priority/assignment

**`PUT /api/v1/admin/support/tickets/{id}`**

```bash
curl -X 'PUT' \
  'http://localhost:8080/api/v1/admin/support/tickets/6c5078d3-f8e3-4c88-9afe-48b5423c664f' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <admin_token>' \
  -H 'Content-Type: application/json' \
  -d '{
  "status": "PENDING",
  "priority": "LOW",
  "category": "ORDER",
  "assignedTo": "string"
}'
```

#### Response

```json
{
  "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "userId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "orderId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "shopId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "subject": "Looks good",
  "category": "ORDER",
  "priority": "LOW",
  "status": "PENDING",
  "assignedTo": "string",
  "lastMessageAt": "2024-05-06T12:00:00",
  "resolvedAt": "2024-05-06T12:00:00",
  "createdAt": "2024-05-06T12:00:00",
  "firstResponseAt": "2024-05-06T12:00:00",
  "responseDueAt": "2024-05-06T12:00:00",
  "resolutionDueAt": "2024-05-06T12:00:00",
  "slaBreached": true,
  "csatScore": 1,
  "csatComment": "string",
  "csatRatedAt": "250.00"
}
```

---

### 13. Admin: download a support attachment

**`GET /api/v1/admin/support/tickets/{id}/attachments/{attachmentId}`**

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/admin/support/tickets/6c5078d3-f8e3-4c88-9afe-48b5423c664f/attachments/6c5078d3-f8e3-4c88-9afe-48b5423c664f' \
  -H 'accept: */*' \
  -H 'Authorization: Bearer <admin_token>'
```

#### Response

```text
<binary attachment content>
```

---
