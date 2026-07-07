# Dashboard Analytics

The Dashboard module provides comprehensive analytics and insights for platform administrators, enabling data-driven decision making through real-time metrics and historical trends.

## Features
- **Summary Statistics**: Quick overview of revenue, orders, users, products, and shops.
- **Revenue Analytics**: Detailed revenue breakdown with daily trends and average order value.
- **Order Statistics**: Order status distribution and recent order activity.
- **User Growth**: User registration trends with breakdown by role and daily signups.
- **Top Products**: Best-selling products ranked by sales volume and revenue.
- **Activity Feed**: Recent platform activity including orders and user registrations.

## Admin API

All dashboard endpoints require `ADMIN` role.

### Get Dashboard Summary
`GET /api/v1/admin/dashboard`

Returns aggregate statistics for the main dashboard view.

**Response:**
```json
{
  "revenue": { "total": "15420.50", "today": "1250.00" },
  "orders": { "total": 342, "today": 15, "pending": 8 },
  "users": { "total": 1250, "today": 5, "sellers": 45 },
  "products": { "total": 3200, "outOfStock": 12, "lowStock": 28 },
  "shops": { "total": 89, "pendingApproval": 3 }
}
```

### Get Revenue Stats
`GET /api/v1/admin/dashboard/revenue?startDate=2024-01-01&endDate=2024-12-31`

Returns revenue data with daily breakdown and average order value for a date range.

**Response:**
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

### Get Order Stats
`GET /api/v1/admin/dashboard/orders?status=PENDING`

Returns order status distribution and recent orders.

**Response:**
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

### Get User Growth
`GET /api/v1/admin/dashboard/users?days=30`

Returns user registration trends over a specified period.

**Response:**
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

### Get Top Products
`GET /api/v1/admin/dashboard/top-products?limit=10`

Returns best-selling products ranked by total sales.

**Response:**
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

### Get Recent Activity
`GET /api/v1/admin/dashboard/activity?limit=20`

Returns a combined feed of recent orders and user registrations.

**Response:**
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
