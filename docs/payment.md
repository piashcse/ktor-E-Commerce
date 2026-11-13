# Payment API

This documentation provides comprehensive details for the Payment API endpoints. The API supports creating and retrieving payments within the platform. Payments are associated with specific orders and support various payment methods for transaction management.

**Base URL:** `http://localhost:8080`

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

---

## Endpoint Details

### 1. Create Payment

**`POST /api/v1/payments`**

Create a new payment intent for an existing order. **The status is owned by the server**:
clients cannot mark a payment `COMPLETED`. Non-COD payments start `PENDING` and are completed
by the provider callback (or an admin confirmation); COD payments follow the order lifecycle.

#### Request Body

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `amount` | number | Yes | Payment amount; must match the order total |
| `orderId` | string | Yes | UUID of the order to pay for |
| `paymentMethod` | string | Yes | Payment method (e.g., "CREDIT_CARD", "BANK_TRANSFER", "PAYPAL", "COD") |
| `transactionId` | string | No | Provider transaction id / idempotency reference |

#### Example Request

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

#### Example Response

```json
{
    "id": "4b68917d-4452-4d18-9012-47e843f05c15",
    "orderId": "7e49b2a1-fa0c-4aac-b996-91f2411f14b7",
    "amount": 500,
    "status": "PENDING",
    "paymentMethod": "BANK_TRANSFER"
}
```

#### Response Fields

| Field | Type | Description |
|-------|------|-------------|
| `id` | string | Unique identifier for the payment |
| `orderId` | string | UUID of the associated order |
| `amount` | number | Payment amount |
| `status` | string | Current payment status |
| `paymentMethod` | string | Payment method used |

---

### 3. Get Payment by ID

**`GET /api/v1/payments/{id}`**

Retrieve details of a specific payment by its ID.

#### Path Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `id` | string | Yes | Unique identifier of the payment |

#### Headers

| Header | Value | Required |
|--------|-------|----------|
| `Authorization` | `Bearer <access_token>` | Yes |

#### Example Request

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/payments/4b68917d-4452-4d18-9012-47e843f05c15' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9...'
```

#### Example Response

```json
{
    "id": "4b68917d-4452-4d18-9012-47e843f05c15",
    "orderId": "7e49b2a1-fa0c-4aac-b996-91f2411f14b7",
    "amount": 500,
    "status": "PENDING",
    "paymentMethod": "BANK_TRANSFER"
}
```

#### Response Fields

| Field | Type | Description |
|-------|------|-------------|
| `id` | string | Unique identifier for the payment |
| `orderId` | string | UUID of the associated order |
| `amount` | number | Payment amount |
| `status` | string | Current payment status |
| `paymentMethod` | string | Payment method used |

---

### 4. Get Payments by Order

**`GET /api/v1/payments/order/{orderId}`**

Retrieve all payments associated with a specific order. This is useful for tracking payment history and partial payments.

#### Path Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `orderId` | string | Yes | UUID of the order |

#### Headers

| Header | Value | Required |
|--------|-------|----------|
| `Authorization` | `Bearer <access_token>` | Yes |

#### Example Request

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/payments/order/7e49b2a1-fa0c-4aac-b996-91f2411f14b7' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9...'
```

#### Example Response

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

#### Response Fields

| Field | Type | Description |
|-------|------|-------------|
| `data` | array | Array of payment objects |
| `data[].id` | string | Unique identifier for the payment |
| `data[].orderId` | string | UUID of the associated order |
| `data[].amount` | number | Payment amount |
| `data[].status` | string | Current payment status |
| `data[].paymentMethod` | string | Payment method used |
| `data[].transactionId` | string | Transaction reference ID (nullable) |

#### Notes

- Payments are returned in descending order by creation date (newest first)
- This endpoint validates that the authenticated user owns the order
- Payment amount is validated against order total during creation

---

## Error Handling

This API follows industry-standard error handling patterns (Stripe, GitHub, OpenAI):

### Success Responses
- **HTTP status code indicates success** (200, 201, 204)
- **Response body contains data directly** (no wrapper object)
- No `isSuccess` or `statusCode` fields needed

### Error Responses

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

## 4. Confirm Payment (Admin)

**`POST /api/v1/admin/payments/{id}/confirm`**

Confirms a `PENDING` payment as `COMPLETED`. This is the server-side/manual confirmation path
used when no provider webhook is configured (for example bank transfers or COD reconciliation).
Repeating the call is idempotent for an already-completed payment.

```bash
curl -X 'POST' \
  'http://localhost:8080/api/v1/admin/payments/4b68917d-4452-4d18-9012-47e843f05c15/confirm' \
  -H 'Authorization: Bearer <admin_token>'
```

---

## 5. Provider Webhook

**`POST /api/v1/payments/webhook/{provider}`**

Public endpoint called by the payment processor. The raw request body and the provider
signature header are verified before anything is written:

- Stripe sends `Stripe-Signature` (`t=<timestamp>,v1=<hmac-sha256>`); the default tolerance is
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

## 6. Release a risk review

**`POST /api/v1/admin/payments/{id}/risk/approve`**

Sets a `REVIEW` payment decision to `ALLOW` and finalizes the order if the payment was already
captured. `BLOCK` payments cannot be released.

---

## Webhook inspection and replay

**`GET /api/v1/admin/webhooks?source=PAYMENT&status=FAILED`**

Lists stored payment and carrier webhook events. Filters: `source` (`PAYMENT`/`CARRIER`), `status`
(`RECEIVED`/`PROCESSED`/`IGNORED`/`FAILED`), `limit`, `offset`.

**`POST /api/v1/admin/webhooks/payments/{id}/replay`**

Replays a stored payment payload. Because the payload was signature-verified on receipt, replay
applies it directly, updates the inbox status/error and writes an audit entry. Use this for provider
outages or ignored events after the original delivery.

**`POST /api/v1/admin/webhooks/carriers/{id}/replay`**

Same flow for carrier tracking payloads.

---

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
