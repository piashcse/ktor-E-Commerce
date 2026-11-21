# Payment

This documentation provides comprehensive details for the Payment API endpoints. The API supports creating and retrieving payments within the platform. Payments are associated with specific orders and support various payment methods for transaction management.

## Authentication

All Payment endpoints require Bearer token authentication. Include the access token in the Authorization header:

```
Authorization: Bearer <your_access_token>
```
### Payment Endpoints

| Method | Endpoint | Description | Authentication Required |
|--------|----------|-------------|------------------------|
| `POST` | `/api/v1/payments` | Create a payment intent for an order (server-owned status) | Yes (Customer) |
| `POST` | `/api/v1/admin/payments/{id}/confirm` | Confirm a pending payment as completed | Yes (Admin) |
| `POST` | `/api/v1/payments/webhook/{provider}` | Provider webhook with signature verification (public) | No |
| `GET` | `/api/v1/admin/payments` | Admin - List payments | Yes |
| `POST` | `/api/v1/admin/payments/{id}/risk` | Record AVS/CVV results for a payment | Yes |
| `POST` | `/api/v1/admin/webhooks/bulk/replay` | Admin: bulk replay webhooks | Yes |
| `GET` | `/api/v1/admin/webhooks/{id}` | Admin: inspect a webhook payload | Yes |

---

## Endpoint Details

### 1. Create Payment

**`POST /api/v1/payments`**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `amount` | number | Yes | Payment amount; must match the order total |
| `orderId` | string | Yes | UUID of the order to pay for |
| `paymentMethod` | string | Yes | Payment method (e.g., "CREDIT_CARD", "BANK_TRANSFER", "PAYPAL", "COD") |
| `transactionId` | string | No | Provider transaction id / idempotency reference |

#### Request

```bash
curl -X 'POST' \
  'http://localhost:8080/api/v1/payments' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9...' \
  -H 'Content-Type: application/json' \
  -d '{
  "amount": 500,
  "orderId": "7e49b2a1-fa0c-4aac-b996-91f2411f14b7",
  "paymentMethod": "BANK_TRANSFER"
}'
```

#### Response

```json
{
    "id": "4b68917d-4452-4d18-9012-47e843f05c15",
    "orderId": "7e49b2a1-fa0c-4aac-b996-91f2411f14b7",
    "amount": 500,
    "status": "PENDING",
    "paymentMethod": "BANK_TRANSFER"
}
```

---

### 2. Get Payment by ID

**`GET /api/v1/payments/{id}`**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `id` | string | Yes | Unique identifier of the payment |

#### Headers

| Header | Value | Required |
|--------|-------|----------|
| `Authorization` | `Bearer <access_token>` | Yes |

#### Request

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/payments/4b68917d-4452-4d18-9012-47e843f05c15' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9...'
```

#### Response

```json
{
    "id": "4b68917d-4452-4d18-9012-47e843f05c15",
    "orderId": "7e49b2a1-fa0c-4aac-b996-91f2411f14b7",
    "amount": 500,
    "status": "PENDING",
    "paymentMethod": "BANK_TRANSFER"
}
```

---

### 3. Get Payments by Order

**`GET /api/v1/payments/order/{orderId}`**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `orderId` | string | Yes | UUID of the order |

#### Headers

| Header | Value | Required |
|--------|-------|----------|
| `Authorization` | `Bearer <access_token>` | Yes |

#### Request

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/payments/order/7e49b2a1-fa0c-4aac-b996-91f2411f14b7' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9...'
```

#### Response

**Status: 200 OK**

```json
[
  {
    "id": "4b68917d-4452-4d18-9012-47e843f05c15",
    "orderId": "7e49b2a1-fa0c-4aac-b996-91f2411f14b7",
    "amount": 500,
    "status": "COMPLETED",
    "paymentMethod": "BANK_TRANSFER",
    "transactionId": "TXN123456"
  }
]
```

#### Notes

- Payments are returned in descending order by creation date (newest first)
- This endpoint validates that the authenticated user owns the order
- Payment amount is validated against order total during creation

---

### 4. Admin - List payments

**`GET /api/v1/admin/payments`**

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/admin/payments?page=1&perPage=20&status=PENDING' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <admin_token>'
```

#### Response

```json
{
  "data": [
    {
      "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "orderId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "amount": "250.00",
      "currency": "USD",
      "status": "PENDING",
      "paymentMethod": "CREDIT_CARD",
      "transactionId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "createdAt": "2024-05-06T12:00:00",
      "provider": "MANUAL",
      "providerReference": "pi_123456789",
      "clientSecret": "pi_123_secret_456",
      "nextActionUrl": "https://example.com/next",
      "scaStatus": "NOT_REQUIRED",
      "providerStatus": "succeeded",
      "riskScore": 1,
      "riskDecision": "ALLOW",
      "avsStatus": "MATCH",
      "cvvStatus": "MATCH",
      "threeDsEnforced": true
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

### 5. Record AVS/CVV results for a payment

**`POST /api/v1/admin/payments/{id}/risk`**

```bash
curl -X 'POST' \
  'http://localhost:8080/api/v1/admin/payments/6c5078d3-f8e3-4c88-9afe-48b5423c664f/risk' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <admin_token>' \
  -H 'Content-Type: application/json' \
  -d '{
  "avsStatus": "MATCH",
  "cvvStatus": "MATCH"
}'
```

#### Response

```json
{
  "message": "Operation completed"
}
```

---

### 6. Admin: bulk replay webhooks

**`POST /api/v1/admin/webhooks/bulk/replay`**

```bash
curl -X 'POST' \
  'http://localhost:8080/api/v1/admin/webhooks/bulk/replay' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <admin_token>' \
  -H 'Content-Type: application/json' \
  -d '{
  "source": "PAYMENT",
  "ids": [
    "6c5078d3-f8e3-4c88-9afe-48b5423c664f"
  ]
}'
```

#### Response

```json
{
  "requested": 1,
  "succeeded": 1,
  "failed": []
}
```

---

### 7. Admin: inspect a webhook payload

**`GET /api/v1/admin/webhooks/{id}`**

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/admin/webhooks/6c5078d3-f8e3-4c88-9afe-48b5423c664f' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <admin_token>'
```

#### Response

```json
{
  "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "source": "PAYMENT",
  "provider": "stripe",
  "eventId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "eventType": "CUSTOMER",
  "status": "RECEIVED",
  "error": "None",
  "payload": "{}",
  "receivedAt": "2024-05-06T12:00:00",
  "processedAt": "2024-05-06T12:00:00"
}
```

---

## Error Handling

This API follows industry-standard error handling patterns (Stripe, GitHub, OpenAI):

### Success Responses
- **HTTP status code indicates success** (200, 201, 204)
- **Response body contains data directly** (no wrapper object)
- No `isSuccess` or `statusCode` fields needed

#### Error Responses

**Standard Error (400/401/403/404/500):**
```json
{
  "message": "Error description"
}
```

**Validation Error (400):**
```json
{
  "message": "Validation failed",
  "errors": [
    {"field": "email", "message": "Invalid email format"},
    {"field": "password", "message": "Password must be at least 8 characters"}
  ]
}
```

### Common Error Codes

| Status Code | Description | Example Message |
|-------------|-------------|-----------------|
| `400` | Bad Request | `"Invalid email or password"` |
| `401` | Unauthorized | `"Authentication required"` |
| `403` | Forbidden | `"Insufficient permissions"` |
| `404` | Not Found | `"Product not found"` |
| `409` | Conflict | `"User already exists with this email"` |
| `500` | Internal Server Error | `"Internal server error"` |

All error messages are centralized and consistent across all endpoints.

---

### 8. Confirm Payment (Admin)

**`POST /api/v1/admin/payments/{id}/confirm`**


```bash
curl -X 'POST' \
  'http://localhost:8080/api/v1/admin/payments/4b68917d-4452-4d18-9012-47e843f05c15/confirm' \
  -H 'Authorization: Bearer <admin_token>'
```

---

### 9. Provider Webhook

**`POST /api/v1/payments/webhook/{provider}`**
  300 seconds and can be tuned with `STRIPE_WEBHOOK_TOLERANCE_SECONDS`.
- Other adapters can use `X-Webhook-Signature`.
- Deliveries are deduplicated by `(provider, event_id)`, so retries are safe.
- `payment_intent.succeeded` completes the payment and marks the order `PAID` once the total is
  covered; `payment_intent.payment_failed` marks it `FAILED`.
- `refund.updated` mirrors the processor refund status onto the refund request.

```bash
curl -X 'POST' \
  'http://localhost:8080/api/v1/payments/webhook/stripe' \
  -H 'Stripe-Signature: t=1700000000,v1=<signature>' \
  -H 'Content-Type: application/json' \
  -d '{"id":"evt_1","type":"payment_intent.succeeded","data":{"object":{"id":"pi_1","status":"succeeded"}}}'
```

Invalid signatures return `400`; duplicate deliveries return `200`. Processing failures return a
5xx so the provider retries rather than the event being permanently dropped.

---

## Provider configuration

| Variable | Description |
|---|---|
| `PAYMENT_PROVIDER` | `manual` (COD only, default) or `stripe` |
| `STRIPE_SECRET_KEY` | Stripe secret key; required when the provider is `stripe` |
| `STRIPE_WEBHOOK_SECRET` | Signing secret used to verify webhooks |
| `STRIPE_BASE_URL` | API base URL (override for tests/proxies) |
| `STRIPE_WEBHOOK_TOLERANCE_SECONDS` | Maximum accepted clock skew for webhook signatures |

`POST /api/v1/payments` also accepts `returnUrl` (where the customer returns after 3DS). Responses
include `provider`, `providerReference`, `clientSecret`, `nextActionUrl`, `scaStatus` and
`providerStatus`. A payment remains `PENDING` while `scaStatus` is `REQUIRED`/`PENDING`.

When a refund is marked `REFUNDED` with `refundMethod=ORIGINAL` and the order has a completed
provider payment, the API issues the refund through the processor and stores
`providerRefundId`/`providerRefundStatus` on the refund request.

---

### 10. Release a risk review

**`POST /api/v1/admin/payments/{id}/risk/approve`**

### 11. Webhook inspection and replay

**`GET /api/v1/admin/webhooks?source=PAYMENT&status=FAILED`**

**`POST /api/v1/admin/webhooks/carriers/{id}/replay`**

## Payment Status Values

### Common Payment Status Values

| Status | Description |
|--------|-------------|
| `PENDING` | Payment has been initiated but not yet processed |
| `COMPLETED` | Payment has been successfully processed |
| `FAILED` | Payment processing failed |
| `REFUNDED` | Payment has been refunded |

---

## Payment Methods

### Supported Payment Methods

| Method | Description |
|--------|-------------|
| `CREDIT_CARD` | Credit/debit card payment |
| `PAYPAL` | PayPal payment |
| `BANK_TRANSFER` | Direct bank transfer |
| `COD` | Cash on delivery |
