# Order API

This documentation provides comprehensive details for the Order API endpoints. The API supports creating, retrieving, and updating orders.

**Base URL:** `http://localhost:8080`

## Authentication

All Order endpoints require Bearer token authentication.

### Order Endpoints

| Method | Endpoint | Description | Authentication Required |
|--------|----------|-------------|------------------------|
| `POST` | `/api/v1/order` | Create a new order | Yes (Customer) |
| `GET` | `/api/v1/order` | Retrieve customer orders | Yes (Customer) |
| `GET` | `/api/v1/seller/order` | Retrieve seller's shop orders | Yes (Seller) |
| `GET` | `/api/v1/admin/order` | Retrieve all orders with filters | Yes (Admin) |
| `PATCH` | `/api/v1/order/status/{id}` | Update order status | Yes |
| `POST` | `/api/v1/order/{id}/cancel` | Cancel an order | Yes |

---

## Endpoint Details

### 1. Create Order

**`POST /api/v1/order`**

Create a new order.

#### Example Request

```bash
curl -X 'POST' \
  'http://localhost:8080/api/v1/order' \
  -H 'Authorization: Bearer <customer_token>' \
  -H 'Content-Type: application/json' \
  -d '{
  "quantity": 1,
  "subTotal": 100,
  "total": 105,
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

---

### 2. Get Seller Orders

**`GET /api/v1/seller/order`**

Retrieve all orders for the authenticated seller's shop.

#### Example Request

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/seller/order?limit=20&status=PENDING' \
  -H 'Authorization: Bearer <seller_token>'
```

---

### 3. Get Admin Orders

**`GET /api/v1/admin/order`**

Retrieve all orders with advanced filters (Admin only).

#### Example Request

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/admin/order?limit=20&status=PENDING' \
  -H 'Authorization: Bearer <admin_token>'
```

---

## Error Handling

| Status Code | Description |
|-------------|-------------|
| `400` | Bad Request |
| `401` | Unauthorized |
| `404` | Not Found |
