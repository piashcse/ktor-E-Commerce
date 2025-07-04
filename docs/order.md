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
| `GET` | `/order` | Retrieve list of orders | Yes |
| `PATCH` | `/order/{id}` | Update order status | Yes |

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

```json
{
  "isSuccess": true,
  "statusCode": {
    "value": 200,
    "description": "OK"
  },
  "data": {
    "orderId": "b177431f-22f2-4c01-8ad6-da5319e2c7b9"
  }
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
| `limit` | number | No | Maximum number of orders to return (default: 10) |
| `offset` | number | No | Number of orders to skip for pagination |

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
  "isSuccess": true,
  "statusCode": {
    "value": 200,
    "description": "OK"
  },
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
    },
    {
      "orderId": "f88ab61d-5e52-431b-82e8-48e5b607085c",
      "quantity": 1,
      "subTotal": 10,
      "total": 10,
      "shippingCharge": 100,
      "cancelOrder": false,
      "status": "pending",
      "statusCode": 0
    },
    {
      "orderId": "b177431f-22f2-4c01-8ad6-da5319e2c7b9",
      "quantity": 1,
      "subTotal": 10,
      "total": 10,
      "shippingCharge": 5,
      "cancelOrder": false,
      "status": "pending",
      "statusCode": 0
    }
  ]
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

---

### 3. Update Order Status

**`PATCH /order/{id}`**

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
  'http://localhost:8080/order/7e49b2a1-fa0c-4aac-b996-91f2411f14b7?status=delivered' \
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
| `404` | Not Found - Order not found |
| `409` | Conflict - Invalid order data or product references |
| `500` | Internal Server Error - Server error |

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
