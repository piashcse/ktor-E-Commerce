# Order API

This documentation provides comprehensive details for the Order API endpoints. The API supports creating, retrieving, and updating orders, with a full audit trail for status changes.

**Base URL:** `http://localhost:8080`

## Authentication

All Order endpoints require Bearer token authentication.

### Order Endpoints

| Method | Endpoint | Description | Authentication Required |
|--------|----------|-------------|------------------------|
| `GET` | `/api/v1/orders` | Retrieve customer orders | Yes (Customer) |
| `PATCH` | `/api/v1/orders/status/{id}` | Update order status (Customer: RECEIVED/CANCELED) | Yes (Customer) |
| `POST` | `/api/v1/orders/{id}/cancel` | Cancel an order with reason | Yes (Customer) |
| `GET` | `/api/v1/seller/orders` | Retrieve seller's shop orders | Yes (Seller) |
| `PATCH` | `/api/v1/admin/orders/status/{id}` | Update any order status | Yes (Admin) |
| `POST` | `/api/v1/admin/orders/{id}/cancel` | Cancel any order | Yes (Admin) |
| `GET` | `/api/v1/admin/orders` | Retrieve all orders with advanced filters | Yes (Admin) |

---

## Endpoint Details

### 1. Get Customer Orders

**`GET /api/v1/orders`**

Retrieve all orders placed by the authenticated customer.

#### Example Request

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/orders?limit=10&offset=0' \
  -H 'Authorization: Bearer <customer_token>'
```

---

### 2. Update Order Status (Customer/Seller)

**`PATCH /api/v1/orders/status/{id}`**

Update the status of an order. The allowed statuses depend on the user role.
- **Customer**: `CANCELED`, `RECEIVED`
- **Seller**: `CONFIRMED`, `DELIVERED`

#### Example Request

```bash
curl -X 'PATCH' \
  'http://localhost:8080/api/v1/orders/status/cbd630f6-bf9f-48ad-ac51-f806807d99fd?status=RECEIVED' \
  -H 'Authorization: Bearer <customer_token>'
```

---

### 3. Cancel Order

**`POST /api/v1/orders/{id}/cancel`**

Cancel an order and provide a reason. This will restore stock for all items in the order.

#### Request Body

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `reason` | string | Yes | Reason for cancellation |

#### Example Request

```bash
curl -X 'POST' \
  'http://localhost:8080/api/v1/orders/cbd630f6-bf9f-48ad-ac51-f806807d99fd/cancel' \
  -H 'Authorization: Bearer <customer_token>' \
  -H 'Content-Type: application/json' \
  -d '{
    "reason": "Changed my mind"
  }'
```

---

### 4. Get Admin Orders

**`GET /api/v1/admin/orders`**

Retrieve all orders with advanced filters.

#### Query Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `status` | string | No | Filter by order status |
| `startDate` | string | No | Filter by start date (ISO-8601) |
| `endDate` | string | No | Filter by end date (ISO-8601) |

---

## Error Handling

| Status Code | Description |
|-------------|-------------|
| `400` | Bad Request (Invalid status transition) |
| `401` | Unauthorized |
| `403` | Forbidden (User does not own the order) |
| `404` | Not Found |
