# Notifications

Notifications are rendered from centralized templates and delivered through a channel dispatcher.

## Channels

| Channel | Behavior |
|---|---|
| `IN_APP` | Always written to the user's notification inbox |
| `EMAIL` | Sent through SMTP (default extra channel) |
| `SMS` | Inert until an SMS provider is configured; skipped events are logged |
| `PUSH` | Inert until a push provider is configured; skipped events are logged |

Domain events currently dispatched: `ORDER_PLACED`, `ORDER_<STATUS>` (shipped/delivered/canceled),
`PAYMENT_COMPLETED`, `BACK_IN_STOCK`, `REFUND_<STATUS>`, `RMA_<STATUS>`, `USER_REGISTERED`.

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
| `POST` | `/api/v1/notifications/send` | Send an announcement (Admin / Seller) |
| `POST` | `/api/v1/seller/notifications/send` | Seller: notify one of my customers |

Sellers/admins can send announcements with `POST /api/v1/notifications/send`
(sellers may only target customers who ordered from their shop).
## Endpoint Details

### 1. Get global notification channel

**`GET /api/v1/notifications/preferences`**

Get global notification channel

#### Request

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/notifications/preferences' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <access_token>'
```

#### Response

```json
{
  "userId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "channel": "EMAIL"
}
```

---

### 2. Set global notification channel

**`PUT /api/v1/notifications/preferences`**

Set global notification channel

#### Request

```bash
curl -X 'PUT' \
  'http://localhost:8080/api/v1/notifications/preferences' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <access_token>' \
  -H 'Content-Type: application/json' \
  -d '{
  "channel": "EMAIL"
}'
```

#### Response

```json
{
  "userId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "channel": "EMAIL"
}
```

---
### 3. Get per-event channel

**`GET /api/v1/notifications/preferences/{eventType}`**

Get per-event channel

#### Request

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/notifications/preferences/6c5078d3-f8e3-4c88-9afe-48b5423c664f' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <access_token>'
```

#### Response

```json
{
  "eventType": "CUSTOMER",
  "channel": "EMAIL"
}
```

---

### 4. Set per-event channel

**`PUT /api/v1/notifications/preferences/{eventType}`**

Set per-event channel

#### Request

```bash
curl -X 'PUT' \
  'http://localhost:8080/api/v1/notifications/preferences/6c5078d3-f8e3-4c88-9afe-48b5423c664f' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <access_token>' \
  -H 'Content-Type: application/json' \
  -d '{
  "channel": "EMAIL"
}'
```

#### Response

```json
{
  "eventType": "CUSTOMER",
  "channel": "EMAIL"
}
```

---

### 5. List notifications

**`GET /api/v1/notifications`**

List notifications

#### Request

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/notifications?page=1&perPage=20' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <access_token>'
```

#### Response

```json
{
  "data": [
    {
      "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "channel": "EMAIL",
      "type": "CUSTOMER",
      "title": "Example",
      "body": "Looks good",
      "resourceType": "CUSTOMER",
      "resourceId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "isRead": true,
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

### 6. Unread notification count

**`GET /api/v1/notifications/unread-count`**

Unread notification count

#### Request

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/notifications/unread-count' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <access_token>'
```

#### Response

```json
{
  "unreadCount": 1
}
```

---

### 7. Cursor-paginated notifications

**`GET /api/v1/notifications/cursor`**

Cursor-paginated notifications

#### Request

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/notifications/cursor?page=1&perPage=20&cursor=cursor-token' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <access_token>'
```

#### Response

```json
{
  "data": [
    {
      "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "channel": "EMAIL",
      "type": "CUSTOMER",
      "title": "Example",
      "body": "Looks good",
      "resourceType": "CUSTOMER",
      "resourceId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "isRead": true,
      "createdAt": "2024-05-06T12:00:00"
    }
  ],
  "nextCursor": "string",
  "hasMore": true,
  "limit": 20
}
```

---

### 8. Mark notification read

**`POST /api/v1/notifications/{id}/read`**

Mark notification read

#### Request

```bash
curl -X 'POST' \
  'http://localhost:8080/api/v1/notifications/6c5078d3-f8e3-4c88-9afe-48b5423c664f/read' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <access_token>'
```

#### Response

```json
{
  "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "channel": "EMAIL",
  "type": "CUSTOMER",
  "title": "Example",
  "body": "Looks good",
  "resourceType": "CUSTOMER",
  "resourceId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "isRead": true,
  "createdAt": "2024-05-06T12:00:00"
}
```

---

### 9. Mark all notifications read

**`POST /api/v1/notifications/read-all`**

Mark all notifications read

#### Request

```bash
curl -X 'POST' \
  'http://localhost:8080/api/v1/notifications/read-all' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <access_token>'
```

#### Response

```json
{
  "markedRead": 1
}
```

---

### 10. Send notification

**`POST /api/v1/notifications/send`**

Send notification

#### Request

```bash
curl -X 'POST' \
  'http://localhost:8080/api/v1/notifications/send' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <access_token>' \
  -H 'Content-Type: application/json' \
  -d '{
  "userId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "title": "Example",
  "body": "Looks good",
  "channel": "EMAIL",
  "type": "CUSTOMER"
}'
```

#### Response

```json
{
  "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "channel": "EMAIL",
  "type": "CUSTOMER",
  "title": "Example",
  "body": "Looks good",
  "resourceType": "CUSTOMER",
  "resourceId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "isRead": true,
  "createdAt": "2024-05-06T12:00:00"
}
```

---

### 11. Seller: notify one of my customers

**`POST /api/v1/seller/notifications/send`**

Seller: notify one of my customers

#### Request

```bash
curl -X 'POST' \
  'http://localhost:8080/api/v1/seller/notifications/send' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <seller_token>' \
  -H 'Content-Type: application/json' \
  -d '{
  "userId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "title": "Example",
  "body": "Looks good",
  "channel": "EMAIL",
  "type": "CUSTOMER"
}'
```

#### Response

```json
{
  "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "channel": "EMAIL",
  "type": "CUSTOMER",
  "title": "Example",
  "body": "Looks good",
  "resourceType": "CUSTOMER",
  "resourceId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "isRead": true,
  "createdAt": "2024-05-06T12:00:00"
}
```

---
