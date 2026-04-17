# Refund Request API

This documentation provides comprehensive details for the Refund Request API endpoints. The API supports creating, retrieving, and managing refund requests for order items. Customers can request refunds, sellers/admins can approve or reject them, and customers can track shipping status for approved refunds.

**Base URL:** `http://localhost:8080`

## Authentication

All Refund Request endpoints require Bearer token authentication. Include the access token in the Authorization header:

```
Authorization: Bearer <your_access_token>
```

### Refund Request Endpoints

| Method | Endpoint | Description | Authentication Required |
|--------|----------|-------------|------------------------|
| `POST` | `/refund-requests/{orderId}` | Create a refund request | Yes (Customer) |
| `GET` | `/refund-requests/order/{orderId}` | Get all refunds for an order | Yes (Customer/Seller/Admin) |
| `GET` | `/refund-requests/{id}` | Get refund request details | Yes |
| `PUT` | `/refund-requests/{id}/status` | Update refund status | Yes (Seller/Admin) |
| `POST` | `/refund-requests/{id}/ship` | Mark refund as shipped | Yes (Customer) |

---

## Endpoint Details

### 1. Create Refund Request

**`POST /refund-requests/{orderId}`**

Create a new refund request for a specific order item. Customers can request refunds for items they've purchased.

#### Path Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `orderId` | string | Yes | UUID of the order |

#### Request Body

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `orderItemId` | string | Yes | UUID of the order item to refund |
| `reason` | string | Yes | Reason for the refund request |
| `images` | string | No | Comma-separated image URLs (evidence) |

#### Headers

| Header | Value | Required |
|--------|-------|----------|
| `Authorization` | `Bearer <access_token>` | Yes |
| `Content-Type` | `application/json` | Yes |

#### Example Request

```bash
curl -X 'POST' \
  'http://localhost:8080/refund-requests/ORD-20260414-0001' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9...' \
  -H 'Content-Type: application/json' \
  -d '{
  "orderItemId": "item-uuid-123",
  "reason": "Product received damaged",
  "images": "https://example.com/img1.jpg,https://example.com/img2.jpg"
}'
```

#### Example Response

**Status: 201 Created**

```json
{
  "id": "refund-uuid-123",
  "orderItemId": "item-uuid-123",
  "orderId": "ORD-20260414-0001",
  "userId": "user-uuid-123",
  "reason": "Product received damaged",
  "images": "https://example.com/img1.jpg,https://example.com/img2.jpg",
  "status": "PENDING",
  "refundAmount": null,
  "refundMethod": null,
  "trackingNumber": null,
  "requestedAt": "2026-04-14T10:30:00",
  "resolvedAt": null,
  "createdAt": "2026-04-14T10:30:00",
  "updatedAt": "2026-04-14T10:30:00"
}
```

#### Response Fields

| Field | Type | Description |
|-------|------|-------------|
| `id` | string | Unique identifier for the refund request |
| `orderItemId` | string | UUID of the order item being refunded |
| `orderId` | string | UUID of the parent order |
| `userId` | string | UUID of the user who requested the refund |
| `reason` | string | Reason for the refund request |
| `images` | string | Comma-separated image URLs (nullable) |
| `status` | string | Current status (PENDING, APPROVED, REJECTED, REFUNDED, SHIPPED) |
| `refundAmount` | number | Approved refund amount (nullable) |
| `refundMethod` | string | Refund method (nullable) |
| `trackingNumber` | string | Return shipping tracking number (nullable) |
| `requestedAt` | string | Timestamp when refund was requested |
| `resolvedAt` | string | Timestamp when refund was resolved (nullable) |
| `createdAt` | string | Timestamp when record was created |
| `updatedAt` | string | Timestamp when record was last updated |

#### Error Responses

| Status Code | Description |
|-------------|-------------|
| `400` | Validation failed or duplicate refund request |
| `401` | Unauthorized - not the order owner |
| `404` | Order or order item not found |

---

### 2. Get Refunds by Order

**`GET /refund-requests/order/{orderId}`**

Retrieve all refund requests for a specific order. Accessible by the order owner, seller, or admin.

#### Path Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `orderId` | string | Yes | UUID of the order |
| `limit` | number | No | Maximum number of refunds to return (default: 20) |
| `offset` | number | No | Number of refunds to skip for pagination (default: 0) |

#### Headers

| Header | Value | Required |
|--------|-------|----------|
| `Authorization` | `Bearer <access_token>` | Yes |

#### Example Request

```bash
curl -X 'GET' \
  'http://localhost:8080/refund-requests/order/ORD-20260414-0001' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9...'
```

#### Example Response

**Status: 200 OK**

```json
{
  "data": [
    {
      "id": "refund-uuid-123",
      "orderItemId": "item-uuid-123",
      "orderId": "ORD-20260414-0001",
      "userId": "user-uuid-123",
      "reason": "Product received damaged",
      "images": "https://example.com/img1.jpg",
      "status": "APPROVED",
      "refundAmount": 50.0,
      "refundMethod": "original_payment",
      "trackingNumber": null,
      "requestedAt": "2026-04-14T10:30:00",
      "resolvedAt": "2026-04-14T12:00:00",
      "createdAt": "2026-04-14T10:30:00",
      "updatedAt": "2026-04-14T12:00:00"
    }
  ],
  "metadata": {
    "totalCount": 1,
    "limit": 20,
    "skip": 0
  }
}
```

#### Permissions

- **Customer**: Can view refunds for their own orders only
- **Seller**: Can view refunds for orders from their shop
- **Admin/Super Admin**: Can view all refunds

---

### 3. Get Refund by ID

**`GET /refund-requests/{id}`**

Retrieve details of a specific refund request by its ID.

#### Path Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `id` | string | Yes | UUID of the refund request |

#### Headers

| Header | Value | Required |
|--------|-------|----------|
| `Authorization` | `Bearer <access_token>` | Yes |

#### Example Request

```bash
curl -X 'GET' \
  'http://localhost:8080/refund-requests/refund-uuid-123' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9...'
```

#### Example Response

**Status: 200 OK**

```json
{
  "id": "refund-uuid-123",
  "orderItemId": "item-uuid-123",
  "orderId": "ORD-20260414-0001",
  "userId": "user-uuid-123",
  "reason": "Product received damaged",
  "images": "https://example.com/img1.jpg",
  "status": "APPROVED",
  "refundAmount": 50.0,
  "refundMethod": "original_payment",
  "trackingNumber": null,
  "requestedAt": "2026-04-14T10:30:00",
  "resolvedAt": "2026-04-14T12:00:00",
  "createdAt": "2026-04-14T10:30:00",
  "updatedAt": "2026-04-14T12:00:00"
}
```

---

### 4. Update Refund Status

**`PUT /refund-requests/{id}/status`**

Update the status of a refund request. Only sellers and admins can approve or reject refunds.

#### Path Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `id` | string | Yes | UUID of the refund request |

#### Request Body

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `status` | string | Yes | New status (APPROVED, REJECTED, REFUNDED) |
| `refundAmount` | number | No | Refund amount (required for REFUNDED status) |
| `refundMethod` | string | No | Refund method (e.g., "original_payment", "bank_transfer") |

#### Headers

| Header | Value | Required |
|--------|-------|----------|
| `Authorization` | `Bearer <access_token>` | Yes |
| `Content-Type` | `application/json` | Yes |

#### Permissions

- **Seller**: Can update refunds for orders from their shop
- **Admin/Super Admin**: Can update any refund

#### Example Request

```bash
curl -X 'PUT' \
  'http://localhost:8080/refund-requests/refund-uuid-123/status' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9...' \
  -H 'Content-Type: application/json' \
  -d '{
  "status": "APPROVED",
  "refundAmount": 50.0,
  "refundMethod": "original_payment"
}'
```

#### Example Response

**Status: 200 OK**

```json
{
  "id": "refund-uuid-123",
  "orderItemId": "item-uuid-123",
  "orderId": "ORD-20260414-0001",
  "userId": "user-uuid-123",
  "reason": "Product received damaged",
  "images": "https://example.com/img1.jpg",
  "status": "APPROVED",
  "refundAmount": 50.0,
  "refundMethod": "original_payment",
  "trackingNumber": null,
  "requestedAt": "2026-04-14T10:30:00",
  "resolvedAt": "2026-04-14T12:00:00",
  "createdAt": "2026-04-14T10:30:00",
  "updatedAt": "2026-04-14T12:00:00"
}
```

#### Valid Status Transitions

| Current Status | Allowed Next Status |
|----------------|---------------------|
| PENDING | APPROVED, REJECTED |
| APPROVED | REFUNDED, SHIPPED |
| REJECTED | (terminal state) |
| REFUNDED | (terminal state) |
| SHIPPED | REFUNDED |

#### Error Responses

| Status Code | Description |
|-------------|-------------|
| `400` | Invalid status or refund not found |
| `403` | Forbidden - insufficient permissions |
| `404` | Refund request not found |

---

### 5. Ship Refund

**`POST /refund-requests/{id}/ship`**

Mark an approved refund as shipped with a tracking number. Only the customer who owns the order can perform this action.

#### Path Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `id` | string | Yes | UUID of the refund request |

#### Request Body

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `trackingNumber` | string | Yes | Return shipping tracking number |

#### Headers

| Header | Value | Required |
|--------|-------|----------|
| `Authorization` | `Bearer <access_token>` | Yes |
| `Content-Type` | `application/json` | Yes |

#### Permissions

- **Customer**: Can only ship their own approved refunds

#### Example Request

```bash
curl -X 'POST' \
  'http://localhost:8080/refund-requests/refund-uuid-123/ship' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9...' \
  -H 'Content-Type: application/json' \
  -d '{
  "trackingNumber": "TRK123456789"
}'
```

#### Example Response

**Status: 200 OK**

```json
{
  "id": "refund-uuid-123",
  "orderItemId": "item-uuid-123",
  "orderId": "ORD-20260414-0001",
  "userId": "user-uuid-123",
  "reason": "Product received damaged",
  "images": "https://example.com/img1.jpg",
  "status": "SHIPPED",
  "refundAmount": 50.0,
  "refundMethod": "original_payment",
  "trackingNumber": "TRK123456789",
  "requestedAt": "2026-04-14T10:30:00",
  "resolvedAt": "2026-04-14T12:00:00",
  "createdAt": "2026-04-14T10:30:00",
  "updatedAt": "2026-04-14T14:00:00"
}
```

#### Error Responses

| Status Code | Description |
|-------------|-------------|
| `400` | Refund not approved or tracking number missing |
| `401` | Unauthorized - not the order owner |
| `404` | Refund request not found |

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
    {"field": "orderItemId", "message": "Order item ID cannot be empty"},
    {"field": "reason", "message": "Reason cannot be empty"}
  ]
}
```

### Common Error Codes

| Status Code | Description | Example Message |
|-------------|-------------|-----------------|
| `400` | Bad Request | `"Refund request already exists for this item"` |
| `401` | Unauthorized | `"Authentication required"` |
| `403` | Forbidden | `"Insufficient permissions"` |
| `404` | Not Found | `"Order item not found"` |
| `500` | Internal Server Error | `"Internal server error"` |

---

## Refund Status Values

### Status Definitions

| Status | Description |
|--------|-------------|
| `PENDING` | Refund request submitted, awaiting review |
| `APPROVED` | Refund approved by seller/admin |
| `REJECTED` | Refund request denied |
| `REFUNDED` | Refund has been processed and paid |
| `SHIPPED` | Customer has shipped item back (for returns) |

---

## Best Practices

1. **Provide Evidence**: Customers should include images showing product defects or issues
2. **Clear Reasons**: Write detailed reasons for refund requests to speed up approval
3. **Track Returns**: Use the ship endpoint to provide tracking for return shipments
4. **Timely Review**: Sellers should review and respond to refund requests promptly
5. **Status Updates**: Use the status endpoint to keep customers informed of progress
