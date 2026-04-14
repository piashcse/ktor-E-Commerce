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
| `POST` | `/payment` | Create a new payment | Yes |
| `GET` | `/payment` | Retrieve user's payments | Yes |
| `GET` | `/payment/{id}` | Retrieve payment by ID | Yes |
| `GET` | `/payment/order/{orderId}` | Retrieve all payments for an order | Yes |

---

## Endpoint Details

### 1. Create Payment

**`POST /payment`**

Create a new payment for an existing order. The payment will be associated with a specific order and include payment method details.

#### Headers

| Header | Value | Required |
|--------|-------|----------|
| `Authorization` | `Bearer <access_token>` | Yes |
| `Content-Type` | `application/json` | Yes |

#### Request Body

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `amount` | number | Yes | Payment amount |
| `orderId` | string | Yes | UUID of the order to pay for |
| `paymentMethod` | string | Yes | Payment method used (e.g., "Bkash", "Card", "Cash") |
| `status` | string | Yes | Payment status (e.g., "COMPLETED", "PENDING", "FAILED") |

#### Example Request

```bash
curl -X 'POST' \
  'http://localhost:8080/payment' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9...' \
  -H 'Content-Type: application/json' \
  -d '{
  "amount": 500,
  "orderId": "7e49b2a1-fa0c-4aac-b996-91f2411f14b7",
  "paymentMethod": "Bkash",
  "status": "COMPLETED"
}'
```

#### Example Response

```json
{
    "id": "4b68917d-4452-4d18-9012-47e843f05c15",
    "orderId": "7e49b2a1-fa0c-4aac-b996-91f2411f14b7",
    "amount": 500,
    "status": "COMPLETED",
    "paymentMethod": "Bkash"
  }
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

### 2. Get User Payments

**`GET /payment`**

Retrieve a list of payments associated with the authenticated user.

#### Query Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `limit` | number | No | Maximum number of payments to return (default: 10) |
| `offset` | number | No | Number of payments to skip for pagination |

#### Headers

| Header | Value | Required |
|--------|-------|----------|
| `Authorization` | `Bearer <access_token>` | Yes |

#### Example Request

```bash
curl -X 'GET' \
  'http://localhost:8080/payment?limit=10' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9...'
```

#### Example Response

```json
[
    {
      "id": "4b68917d-4452-4d18-9012-47e843f05c15",
      "orderId": "7e49b2a1-fa0c-4aac-b996-91f2411f14b7",
      "amount": 500,
      "status": "COMPLETED",
      "paymentMethod": "Bkash"
    }
  ]
}
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

---

### 3. Get Payment by ID

**`GET /payment/{id}`**

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
  'http://localhost:8080/payment/4b68917d-4452-4d18-9012-47e843f05c15' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9...'
```

#### Example Response

```json
{
    "id": "4b68917d-4452-4d18-9012-47e843f05c15",
    "orderId": "7e49b2a1-fa0c-4aac-b996-91f2411f14b7",
    "amount": 500,
    "status": "COMPLETED",
    "paymentMethod": "Bkash"
  }
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

**`GET /payment/order/{orderId}`**

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
  'http://localhost:8080/payment/order/7e49b2a1-fa0c-4aac-b996-91f2411f14b7' \
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
    "paymentMethod": "Bkash",
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

## Payment Status Values

### Common Payment Status Values

| Status | Description |
|--------|-------------|
| `PENDING` | Payment has been initiated but not yet processed |
| `COMPLETED` | Payment has been successfully processed |
| `FAILED` | Payment processing failed |
| `CANCELLED` | Payment was cancelled |
| `REFUNDED` | Payment has been refunded |

---

## Payment Methods

### Supported Payment Methods

| Method | Description |
|--------|-------------|
| `Bkash` | Mobile financial service payment |
| `Card` | Credit/Debit card payment |
| `Cash` | Cash on delivery payment |
| `Bank Transfer` | Direct bank transfer |
| `Nagad` | Mobile financial service payment |
| `Rocket` | Mobile financial service payment |
