# Order API

This documentation provides comprehensive details for the Order API endpoints. The API supports creating, retrieving, and updating orders within the platform. Orders contain order items with associated products and can be managed by customers and administrators.

**Base URL:** `http://localhost:8080`

## Authentication

All Order endpoints require Bearer token authentication. Include the access token in the Authorization header:

```
Authorization: Bearer <your_access_token>
```

### Order Endpoints

| Method | Endpoint | Description | Authentication Required |
|--------|----------|-------------|------------------------|
| `POST` | `/order` | Create a new order | Yes |
| `GET` | `/order` | Retrieve customer orders | Yes |
| `GET` | `/order/seller` | Retrieve seller's shop orders | Yes (Seller) |
| `GET` | `/order/admin` | Retrieve all orders with filters | Yes (Admin) |
| `PATCH` | `/order/status/{id}` | Update order status | Yes |
| `POST` | `/order/{id}/cancel` | Cancel an order | Yes |

---

## Endpoint Details

### 1. Create Order

**`POST /order`**

Create a new order with specified items, quantities, and pricing details. The order will be created with order items containing product references.

#### Headers

| Header | Value | Required |
|--------|-------|----------|
| `Authorization` | `Bearer <access_token>` | Yes |
| `Content-Type` | `application/json` | Yes |

#### Request Body

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `quantity` | number | Yes | Total quantity of items in the order |
| `subTotal` | number | Yes | Subtotal amount before shipping |
| `total` | number | Yes | Total order amount including shipping |
| `shippingCharge` | number | Yes | Shipping cost for the order |
| `orderStatus` | string | Yes | Initial status of the order (e.g., "pending") |
| `orderItems` | array | Yes | Array of order items |
| `orderItems[].productId` | string | Yes | UUID of the product to order |
| `orderItems[].quantity` | number | Yes | Quantity of the specific product |

#### Example Request

```bash
curl -X 'POST' \
  'http://localhost:8080/order' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9...' \
  -H 'Content-Type: application/json' \
  -d '{
  "quantity": 1,
  "subTotal": 10,
  "total": 10,
  "shippingCharge": 5,
  "orderStatus": "pending",
  "orderItems": [
    {
      "productId": "71b26dd9-b4b5-4f87-a84d-c8daa506018a",
      "quantity": 1
    }
  ]
}'
```

#### Example Response

**Status: 201 Created**

```json
{
  "orderId": "b177431f-22f2-4c01-8ad6-da5319e2c7b9"
}
```

#### Response Fields

| Field | Type | Description |
|-------|------|-------------|
| `orderId` | string | Unique identifier for the created order |

---

### 2. Get Orders

**`GET /order`**

Retrieve a list of orders with optional pagination support.

#### Query Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `limit` | number | No | Maximum number of orders to return (default: 20) |
| `offset` | number | No | Number of orders to skip (default: 0) |

#### Headers

| Header | Value | Required |
|--------|-------|----------|
| `Authorization` | `Bearer <access_token>` | Yes |

#### Example Request

```bash
curl -X 'GET' \
  'http://localhost:8080/order?limit=10' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9...'
```

#### Example Response

```json
{
  "data": [
    {
      "orderId": "04675b54-a9df-4200-a526-0b15f6a85930",
      "quantity": 1,
      "subTotal": 10,
      "total": 10,
      "shippingCharge": 100,
      "cancelOrder": false,
      "status": "pending",
      "statusCode": 0
    }
  ],
  "metadata": {
    "totalCount": 15,
    "limit": 10,
    "skip": 0
  }
}
```

#### Response Fields

| Field | Type | Description |
|-------|------|-------------|
| `data` | array | Array of order objects |
| `data[].orderId` | string | Unique identifier for the order |
| `data[].quantity` | number | Total quantity of items in the order |
| `data[].subTotal` | number | Subtotal amount before shipping |
| `data[].total` | number | Total order amount including shipping |
| `data[].shippingCharge` | number | Shipping cost for the order |
| `data[].cancelOrder` | boolean | Whether the order is cancelled |
| `data[].status` | string | Current status of the order |
| `data[].statusCode` | number | Numeric status code for the order |
| `metadata` | object | Pagination metadata |
| `metadata.totalCount` | number | Total number of items matching filters |
| `metadata.limit` | number | Maximum number of items requested |
| `metadata.skip` | number | Number of items skipped |

---

### 3. Update Order Status

**`PATCH /order/status/{id}`**

Update the status of an existing order by its ID. This endpoint allows changing the order status (e.g., from pending to delivered).

#### Path Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `id` | string | Yes | Unique identifier of the order to update |

#### Query Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `status` | string | Yes | New status for the order |

#### Headers

| Header | Value | Required |
|--------|-------|----------|
| `Authorization` | `Bearer <access_token>` | Yes |

#### Example Request

```bash
curl -X 'PATCH' \
  'http://localhost:8080/order/status/7e49b2a1-fa0c-4aac-b996-91f2411f14b7?status=delivered' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9...'
```

#### Example Response

```json
{
    "orderId": "7e49b2a1-fa0c-4aac-b996-91f2411f14b7",
    "quantity": 1073741824,
    "subTotal": 0.1,
    "total": 0.1,
    "shippingCharge": 0.1,
    "cancelOrder": false,
    "status": "delivered",
    "statusCode": 4
  }
}
```

#### Response Fields

| Field | Type | Description |
|-------|------|-------------|
| `orderId` | string | Unique identifier of the updated order |
| `quantity` | number | Total quantity of items in the order |
| `subTotal` | number | Subtotal amount before shipping |
| `total` | number | Total order amount including shipping |
| `shippingCharge` | number | Shipping cost for the order |
| `cancelOrder` | boolean | Whether the order is cancelled |
| `status` | string | Updated status of the order |
| `statusCode` | number | Numeric status code for the order |

---

### 4. Cancel Order

**`POST /order/{id}/cancel`**

Cancel an existing order. Only orders with PENDING or CONFIRMED status can be cancelled. This endpoint automatically restores stock quantities.

#### Path Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `id` | string | Yes | Unique identifier of the order to cancel |

#### Request Body

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `reason` | string | Yes | Reason for cancellation (max 500 characters) |

#### Headers

| Header | Value | Required |
|--------|-------|----------|
| `Authorization` | `Bearer <access_token>` | Yes |
| `Content-Type` | `application/json` | Yes |

#### Permissions

- **Customer**: Can cancel their own orders only
- **Seller**: Can cancel orders from their shop
- **Admin/Super Admin**: Can cancel any order

#### Example Request

```bash
curl -X 'POST' \
  'http://localhost:8080/order/7e49b2a1-fa0c-4aac-b996-91f2411f14b7/cancel' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9...' \
  -H 'Content-Type: application/json' \
  -d '{
  "reason": "Customer requested cancellation"
}'
```

#### Example Response

**Status: 200 OK**

```json
{
  "orderId": "7e49b2a1-fa0c-4aac-b996-91f2411f14b7",
  "subTotal": 10.0,
  "total": 10.0,
  "status": "CANCELED"
}
```

#### Response Fields

| Field | Type | Description |
|-------|------|-------------|
| `orderId` | string | Unique identifier of the cancelled order |
| `subTotal` | number | Subtotal amount before shipping |
| `total` | number | Total order amount |
| `status` | string | Updated status (CANCELED) |

#### Error Responses

| Status Code | Description |
|-------------|-------------|
| `400` | Order cannot be cancelled in current status |
| `401` | Unauthorized - not the order owner |
| `404` | Order not found |

---

### 5. Get Seller Orders

**`GET /order/seller`**

Retrieve all orders for the authenticated seller's shop with optional status filter and pagination.

#### Query Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `limit` | number | Yes | Maximum number of orders to return |
| `offset` | number | No | Number of orders to skip (default: 0) |
| `status` | string | No | Filter by order status |

#### Headers

| Header | Value | Required |
|--------|-------|----------|
| `Authorization` | `Bearer <access_token>` | Yes |

#### Example Request

```bash
curl -X 'GET' \
  'http://localhost:8080/order/seller?limit=20&offset=0&status=PENDING' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9...'
```

#### Example Response

```json
{
  "data": [
    {
      "orderId": "ORD-20260414-0001",
      "subTotal": 50.0,
      "total": 50.0,
      "status": "PENDING"
    }
  ],
  "metadata": {
    "totalCount": 1,
    "limit": 20,
    "skip": 0
  }
}
```

---

### 6. Get Admin Orders

**`GET /order/admin`**

Retrieve all orders with advanced filters for admin users. Returns orders with pagination and total count.

#### Query Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `limit` | number | Yes | Maximum number of orders to return |
| `offset` | number | No | Number of orders to skip (default: 0) |
| `status` | string | No | Filter by order status |
| `startDate` | string | No | Filter by start date (ISO 8601 format) |
| `endDate` | string | No | Filter by end date (ISO 8601 format) |

#### Headers

| Header | Value | Required |
|--------|-------|----------|
| `Authorization` | `Bearer <access_token>` | Yes |

#### Example Request

```bash
curl -X 'GET' \
  'http://localhost:8080/order/admin?limit=20&offset=0&status=PENDING&startDate=2026-04-01T00:00:00Z' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9...'
```

#### Example Response

```json
{
  "data": [
    {
      "orderId": "ORD-20260414-0001",
      "subTotal": 50.0,
      "total": 50.0,
      "status": "PENDING"
    }
  ],
  "metadata": {
    "totalCount": 150,
    "limit": 20,
    "skip": 0
  }
}
```

#### Response Fields

| Field | Type | Description |
|-------|------|-------------|
| `orders` | array | Array of order objects |
| `total` | number | Total number of orders matching filters |
| `page` | number | Current page number |
| `limit` | number | Number of orders per page |

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

## Order Status Values

### Common Status Values

| Status | Status Code | Description |
|--------|-------------|-------------|
| `pending` | 0 | Order has been created but not processed |
| `confirmed` | 1 | Order has been confirmed |
| `processing` | 2 | Order is being processed |
| `shipped` | 3 | Order has been shipped |
| `delivered` | 4 | Order has been delivered |
| `cancelled` | 5 | Order has been cancelled |
