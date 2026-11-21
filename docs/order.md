# Order API

This documentation provides comprehensive details for the Order API endpoints. Tax is resolved per item from `tax_rule` (country/category) with a 5% fallback. The API supports creating, retrieving, and updating orders, with a full audit trail for status changes.

**Base URL:** `http://localhost:8080`

## Authentication

All Order endpoints require Bearer token authentication.

### Order Endpoints

| Method | Endpoint | Description | Authentication Required |
|--------|----------|-------------|------------------------|
| `GET` | `/api/v1/orders` | Retrieve customer orders (offset pagination) | Yes (Customer) |
| `GET` | `/api/v1/orders/cursor` | Retrieve customer orders with keyset pagination (`cursor` + `limit`) | Yes (Customer) |
| `GET` | `/api/v1/orders/track` | Public guest tracking (`orderNumber` + `email`) | No |
| `PATCH` | `/api/v1/orders/status/{id}` | Update status (Customer: RECEIVED/CANCELED; Seller: CONFIRMED/SHIPPED/DELIVERED) | Yes |
| `POST` | `/api/v1/orders/{id}/cancel` | Cancel an order with reason | Yes (Customer) |
| `POST` | `/api/v1/orders/{id}/shipping` | Seller: mark a paid order SHIPPED with tracking | Yes (Seller) |
| `POST` | `/api/v1/admin/orders/{id}/shipping` | Admin: mark a paid order SHIPPED with tracking | Yes (Admin) |
| `GET` | `/api/v1/seller/orders` | Retrieve seller's shop orders | Yes (Seller) |
| `PATCH` | `/api/v1/admin/orders/status/{id}` | Update any order status | Yes (Admin) |
| `POST` | `/api/v1/admin/orders/{id}/cancel` | Cancel any order | Yes (Admin) |
| `GET` | `/api/v1/admin/orders` | Retrieve all orders with advanced filters | Yes (Admin) |
| `GET` | `/api/v1/orders/{id}/invoice` | Printable HTML invoice (owner/seller/admin) | Yes |
| `GET` | `/api/v1/orders/{id}/packing-slip` | Printable HTML packing slip | Yes |
| `GET` | `/api/v1/admin/orders/export` | Admin: export orders CSV | Yes |
| `GET` | `/api/v1/admin/orders/{id}` | Admin: get order by ID | Yes |
| `GET` | `/api/v1/orders/track/invoice` | Guest order invoice | No |
| `GET` | `/api/v1/orders/track/timeline` | Guest order timeline | No |
| `GET` | `/api/v1/orders/{id}` | Get order by ID | Yes |
| `GET` | `/api/v1/orders/{id}/shipments` | List order shipments | Yes |
| `POST` | `/api/v1/orders/{id}/shipments` | Purchase a shipping label | Yes |
| `POST` | `/api/v1/orders/{id}/shipping-quotes` | Get shipping rate quotes | Yes |
| `GET` | `/api/v1/seller/orders/export` | Seller: export orders CSV | Yes |
| `GET` | `/api/v1/seller/orders/{id}` | Seller: get order by ID | Yes |
| `POST` | `/api/v1/seller/orders/{id}/cancel` | Seller: cancel order | Yes |
| `POST` | `/api/v1/seller/orders/{id}/pickup/complete` | Seller: verify pickup code and complete collection | Yes |
| `POST` | `/api/v1/seller/orders/{id}/pickup/ready` | Seller: mark a pickup order ready | Yes |
| `POST` | `/api/v1/seller/orders/{id}/shipping` | Seller: mark order shipped | Yes |

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
- **Seller**: `CONFIRMED`, `SHIPPED`, `DELIVERED`

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
