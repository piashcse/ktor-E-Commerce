# Dashboard Analytics API

This documentation provides comprehensive details for the Dashboard Analytics API endpoints. The API provides aggregate statistics and insights for platform administrators.

**Base URL:** `http://localhost:8080`

## Authentication

All Dashboard endpoints require `ADMIN` role.

### Dashboard Endpoints

| Method | Endpoint | Description | Authentication Required |
|--------|----------|-------------|------------------------|
| `GET` | `/api/v1/admin/dashboard` | Get dashboard summary | Yes (Admin) |
| `GET` | `/api/v1/admin/dashboard/revenue` | Get revenue analytics | Yes (Admin) |
| `GET` | `/api/v1/admin/dashboard/orders` | Get order statistics | Yes (Admin) |
| `GET` | `/api/v1/admin/dashboard/users` | Get user growth trends | Yes (Admin) |
| `GET` | `/api/v1/admin/dashboard/top-products` | Get top selling products | Yes (Admin) |
| `GET` | `/api/v1/admin/dashboard/activity` | Get recent activity feed | Yes (Admin) |

---

## Endpoint Details

### 1. Get Dashboard Summary

**`GET /api/v1/admin/dashboard`**

Returns aggregate statistics for the main dashboard view including revenue, orders, users, products, and shops.

#### Example Request

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/admin/dashboard' \
  -H 'Authorization: Bearer <admin_token>'
```

#### Example Response

```json
{
  "revenue": { "total": "15420.50", "today": "1250.00" },
  "orders": { "total": 342, "today": 15, "pending": 8 },
  "users": { "total": 1250, "today": 5, "sellers": 45 },
  "products": { "total": 3200, "outOfStock": 12, "lowStock": 28 },
  "shops": { "total": 89, "pendingApproval": 3 }
}
```

---

### 2. Get Revenue Stats

**`GET /api/v1/admin/dashboard/revenue`**

Returns revenue data with daily breakdown and average order value for a date range.

#### Query Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `startDate` | string | No | Start date (YYYY-MM-DD) |
| `endDate` | string | No | End date (YYYY-MM-DD) |

#### Example Request

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/admin/dashboard/revenue?startDate=2024-01-01&endDate=2024-12-31' \
  -H 'Authorization: Bearer <admin_token>'
```

#### Example Response

```json
{
  "totalRevenue": "15420.50",
  "totalOrders": 342,
  "averageOrderValue": "45.09",
  "dailyRevenue": [
    { "date": "2024-01-01", "revenue": "520.00" },
    { "date": "2024-01-02", "revenue": "380.00" }
  ],
  "currency": "USD"
}
```

---

### 3. Get Order Stats

**`GET /api/v1/admin/dashboard/orders`**

Returns order status distribution and recent orders.

#### Query Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `status` | string | No | Filter by order status (e.g., `PENDING`, `PAID`) |

#### Example Request

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/admin/dashboard/orders?status=PENDING' \
  -H 'Authorization: Bearer <admin_token>'
```

#### Example Response

```json
{
  "statusDistribution": {
    "pending": 8, "confirmed": 15, "paid": 120,
    "delivered": 180, "canceled": 10, "received": 9
  },
  "recentOrders": [
    {
      "orderNumber": "ORD-20240707-0001-ABC123",
      "status": "pending",
      "total": "99.99",
      "createdAt": "2024-07-07T10:30:00"
    }
  ]
}
```

---

### 4. Get User Growth

**`GET /api/v1/admin/dashboard/users`**

Returns user registration trends over a specified period.

#### Query Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `days` | int | No | Number of days to look back (default: 30) |

#### Example Request

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/admin/dashboard/users?days=30' \
  -H 'Authorization: Bearer <admin_token>'
```

#### Example Response

```json
{
  "totalUsers": 1250,
  "newUsersInPeriod": 45,
  "periodDays": 30,
  "byUserType": { "customer": 1100, "seller": 45, "admin": 5, "super_admin": 1 },
  "dailySignups": [
    { "date": "2024-06-07", "count": 3 },
    { "date": "2024-06-08", "count": 1 }
  ]
}
```

---

### 5. Get Top Products

**`GET /api/v1/admin/dashboard/top-products`**

Returns best-selling products ranked by total sales.

#### Query Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `limit` | int | No | Number of top products to return (default: 10) |

#### Example Request

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/admin/dashboard/top-products?limit=10' \
  -H 'Authorization: Bearer <admin_token>'
```

#### Example Response

```json
[
  {
    "productId": "uuid",
    "name": "Galaxy S24",
    "sku": "SAMSUNG-S24-BLK",
    "totalSales": 150,
    "totalRevenue": "149985.00",
    "stockQuantity": 50,
    "rating": "4.50",
    "status": "active"
  }
]
```

---

### 6. Get Recent Activity

**`GET /api/v1/admin/dashboard/activity`**

Returns a combined feed of recent orders and user registrations.

#### Query Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `limit` | int | No | Number of activities to return (default: 20) |

#### Example Request

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/admin/dashboard/activity?limit=20' \
  -H 'Authorization: Bearer <admin_token>'
```

#### Example Response

```json
[
  {
    "id": "uuid",
    "type": "order",
    "summary": "Order ORD-20240707-0001-ABC123 created - $99.99",
    "status": "pending",
    "createdAt": "2024-07-07T10:30:00"
  },
  {
    "id": "uuid",
    "type": "user",
    "summary": "New seller registered: seller@example.com",
    "status": "verified",
    "createdAt": "2024-07-07T09:15:00"
  }
]
```

---

## Error Handling

| Status Code | Description |
|-------------|-------------|
| `400` | Bad Request |
| `401` | Unauthorized |
| `403` | Forbidden |
