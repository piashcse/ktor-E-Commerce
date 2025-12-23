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
  "isSuccess": true,
  "statusCode": {
    "value": 200,
    "description": "OK"
  },
  "data": {
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
{
  "isSuccess": true,
  "statusCode": {
    "value": 200,
    "description": "OK"
  },
  "data": [
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
  "isSuccess": true,
  "statusCode": {
    "value": 200,
    "description": "OK"
  },
  "data": {
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

## Response Format

All API responses follow a consistent format:

```json
{
  "isSuccess": boolean,
  "statusCode": {
    "value": number,
    "description": string
  },
  "data": any
}
```

### Response Fields

| Field | Type | Description |
|-------|------|-------------|
| `isSuccess` | boolean | Indicates if the operation was successful |
| `statusCode.value` | number | HTTP status code |
| `statusCode.description` | string | HTTP status description |
| `data` | any | Response data (varies by endpoint) |

---

## Error Handling

The API returns appropriate HTTP status codes and error messages:

| Status Code | Description |
|-------------|-------------|
| `200` | OK - Request successful |
| `400` | Bad Request - Invalid parameters or missing required fields |
| `401` | Unauthorized - Invalid or missing authentication |
| `403` | Forbidden - Insufficient privileges |
| `404` | Not Found - Payment or order not found |
| `409` | Conflict - Payment already exists or invalid order reference |
| `500` | Internal Server Error - Server error |

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
