# Order

Order creation, status transitions, tracking, invoices and packing slips.

## Authentication

All Order endpoints require Bearer token authentication.

### Order Endpoints

| Method | Endpoint | Description | Authentication Required |
|--------|----------|-------------|------------------------|
| <span class="method-badge get">GET</span> | `/api/v1/orders` | Retrieve customer orders (offset pagination) | Yes (Customer) |
| <span class="method-badge get">GET</span> | `/api/v1/orders/cursor` | Retrieve customer orders with keyset pagination (`cursor` + `limit`) | Yes (Customer) |
| <span class="method-badge get">GET</span> | `/api/v1/orders/track` | Public guest tracking (`orderNumber` + `email`) | No |
| <span class="method-badge patch">PATCH</span> | `/api/v1/orders/status/{id}` | Update status (Customer: RECEIVED/CANCELED; Seller: CONFIRMED/SHIPPED/DELIVERED) | Yes |
| <span class="method-badge post">POST</span> | `/api/v1/orders/{id}/cancel` | Cancel an order with reason | Yes (Customer) |
| <span class="method-badge post">POST</span> | `/api/v1/orders/{id}/shipping` | Seller: mark a paid order SHIPPED with tracking | Yes (Seller) |
| <span class="method-badge post">POST</span> | `/api/v1/admin/orders/{id}/shipping` | Admin: mark a paid order SHIPPED with tracking | Yes (Admin) |
| <span class="method-badge get">GET</span> | `/api/v1/seller/orders` | Retrieve seller's shop orders | Yes (Seller) |
| <span class="method-badge patch">PATCH</span> | `/api/v1/admin/orders/status/{id}` | Update any order status | Yes (Admin) |
| <span class="method-badge post">POST</span> | `/api/v1/admin/orders/{id}/cancel` | Cancel any order | Yes (Admin) |
| <span class="method-badge get">GET</span> | `/api/v1/admin/orders` | Retrieve all orders with advanced filters | Yes (Admin) |
| <span class="method-badge get">GET</span> | `/api/v1/orders/{id}/invoice` | Printable HTML invoice (owner/seller/admin) | Yes |
| <span class="method-badge get">GET</span> | `/api/v1/orders/{id}/packing-slip` | Printable HTML packing slip | Yes |
| <span class="method-badge get">GET</span> | `/api/v1/admin/orders/export` | Admin: export orders CSV | Yes |
| <span class="method-badge get">GET</span> | `/api/v1/admin/orders/{id}` | Admin: get order by ID | Yes |
| <span class="method-badge get">GET</span> | `/api/v1/orders/track/invoice` | Guest order invoice | No |
| <span class="method-badge get">GET</span> | `/api/v1/orders/track/timeline` | Guest order timeline | No |
| <span class="method-badge get">GET</span> | `/api/v1/orders/{id}` | Get order by ID | Yes |
| <span class="method-badge get">GET</span> | `/api/v1/orders/{id}/shipments` | List order shipments | Yes |
| <span class="method-badge post">POST</span> | `/api/v1/orders/{id}/shipments` | Purchase a shipping label | Yes |
| <span class="method-badge post">POST</span> | `/api/v1/orders/{id}/shipping-quotes` | Get shipping rate quotes | Yes |
| <span class="method-badge get">GET</span> | `/api/v1/seller/orders/export` | Seller: export orders CSV | Yes |
| <span class="method-badge get">GET</span> | `/api/v1/seller/orders/{id}` | Seller: get order by ID | Yes |
| <span class="method-badge post">POST</span> | `/api/v1/seller/orders/{id}/cancel` | Seller: cancel order | Yes |
| <span class="method-badge post">POST</span> | `/api/v1/seller/orders/{id}/pickup/complete` | Seller: verify pickup code and complete collection | Yes |
| <span class="method-badge post">POST</span> | `/api/v1/seller/orders/{id}/pickup/ready` | Seller: mark a pickup order ready | Yes |
| <span class="method-badge post">POST</span> | `/api/v1/seller/orders/{id}/shipping` | Seller: mark order shipped | Yes |

---

## Endpoint Details

### 1. Get Customer Orders

<div class="endpoint-header">
  <span class="method-badge get">GET</span>
  <span class="endpoint-path">/api/v1/orders</span>
</div>

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/orders?limit=10&offset=0' \
  -H 'Authorization: Bearer <customer_token>'
```

---

### 2. Update Order Status (Customer/Seller)

<div class="endpoint-header">
  <span class="method-badge patch">PATCH</span>
  <span class="endpoint-path">/api/v1/orders/status/{id}</span>
</div>

- **Seller**: `CONFIRMED`, `SHIPPED`, `DELIVERED`

#### Request

```bash
curl -X 'PATCH' \
  'http://localhost:8080/api/v1/orders/status/cbd630f6-bf9f-48ad-ac51-f806807d99fd?status=RECEIVED' \
  -H 'Authorization: Bearer <customer_token>'
```

---

### 3. Cancel Order

<div class="endpoint-header">
  <span class="method-badge post">POST</span>
  <span class="endpoint-path">/api/v1/orders/{id}/cancel</span>
</div>

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `reason` | string | Yes | Reason for cancellation |

#### Request

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

<div class="endpoint-header">
  <span class="method-badge get">GET</span>
  <span class="endpoint-path">/api/v1/admin/orders</span>
</div>

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `status` | string | No | Filter by order status |
| `startDate` | string | No | Filter by start date (ISO-8601) |
| `endDate` | string | No | Filter by end date (ISO-8601) |

---

### 5. Cursor-paginated order history

<div class="endpoint-header">
  <span class="method-badge get">GET</span>
  <span class="endpoint-path">/api/v1/orders/cursor</span>
</div>

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/orders/cursor?page=1&perPage=20&cursor=cursor-token' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <access_token>'
```

#### Response

```json
{
  "data": [
    {
      "orderId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "orderNumber": "ORD-20240506-0001-A1B2C3D4",
      "userId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "shopId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "subTotal": "250.00",
      "shippingCost": "25.00",
      "taxAmount": "250.00",
      "discountAmount": "250.00",
      "total": "250.00",
      "currency": "USD",
      "status": "PENDING",
      "allowedActions": [
        "APPROVE"
      ],
      "nextStatuses": [
        "SHIPPED"
      ],
      "paymentStatus": "PENDING",
      "couponCode": "SAVE10",
      "paymentMethod": "CREDIT_CARD",
      "notes": "Looks good",
      "contactEmail": "jane@example.com",
      "shippingAddress": "123 Main St",
      "billingAddress": "123 Main St"
    }
  ],
  "nextCursor": "string",
  "hasMore": true,
  "limit": 20
}
```

---

### 6. Guest order tracking

<div class="endpoint-header">
  <span class="method-badge get">GET</span>
  <span class="endpoint-path">/api/v1/orders/track</span>
</div>

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/orders/track?orderNumber=ORD-20240506-0001-A1B2C3D4&email=jane@example.com' \
  -H 'accept: application/json'
```

#### Response

```json
{
  "orderNumber": "ORD-20240506-0001-A1B2C3D4",
  "status": "PENDING",
  "paymentStatus": "PENDING",
  "total": "250.00",
  "currency": "USD",
  "itemCount": 1,
  "items": [
    {
      "productName": "Wireless Headphones",
      "variantName": "string",
      "quantity": 1,
      "total": "250.00"
    }
  ],
  "trackingNumber": "1Z999AA10123456784",
  "shippingDate": "2024-05-06T12:00:00",
  "deliveredDate": "2024-05-06T12:00:00",
  "canceledDate": "2024-05-06T12:00:00",
  "completedDate": "2024-05-06T12:00:00",
  "fulfillmentType": "DELIVERY",
  "pickupStatus": "PENDING"
}
```

---

### 7. Seller: mark order shipped

<div class="endpoint-header">
  <span class="method-badge post">POST</span>
  <span class="endpoint-path">/api/v1/orders/{id}/shipping</span>
</div>

```bash
curl -X 'POST' \
  'http://localhost:8080/api/v1/orders/6c5078d3-f8e3-4c88-9afe-48b5423c664f/shipping' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <access_token>' \
  -H 'Content-Type: application/json' \
  -d '{
  "trackingNumber": "1Z999AA10123456784"
}'
```

#### Response

```json
{
  "orderId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "orderNumber": "ORD-20240506-0001-A1B2C3D4",
  "userId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "shopId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "subTotal": "250.00",
  "shippingCost": "25.00",
  "taxAmount": "250.00",
  "discountAmount": "250.00",
  "total": "250.00",
  "currency": "USD",
  "status": "PENDING",
  "allowedActions": [
    "APPROVE"
  ],
  "nextStatuses": [
    "SHIPPED"
  ],
  "paymentStatus": "PENDING",
  "couponCode": "SAVE10",
  "paymentMethod": "CREDIT_CARD",
  "notes": "Looks good",
  "contactEmail": "jane@example.com",
  "shippingAddress": "123 Main St",
  "billingAddress": "123 Main St"
}
```

---

### 8. Admin - Mark a paid order as shipped

<div class="endpoint-header">
  <span class="method-badge post">POST</span>
  <span class="endpoint-path">/api/v1/admin/orders/{id}/shipping</span>
</div>

```bash
curl -X 'POST' \
  'http://localhost:8080/api/v1/admin/orders/6c5078d3-f8e3-4c88-9afe-48b5423c664f/shipping' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <admin_token>' \
  -H 'Content-Type: application/json' \
  -d '{
  "trackingNumber": "1Z999AA10123456784"
}'
```

#### Response

```json
{
  "orderId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "orderNumber": "ORD-20240506-0001-A1B2C3D4",
  "userId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "shopId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "subTotal": "250.00",
  "shippingCost": "25.00",
  "taxAmount": "250.00",
  "discountAmount": "250.00",
  "total": "250.00",
  "currency": "USD",
  "status": "PENDING",
  "allowedActions": [
    "APPROVE"
  ],
  "nextStatuses": [
    "SHIPPED"
  ],
  "paymentStatus": "PENDING",
  "couponCode": "SAVE10",
  "paymentMethod": "CREDIT_CARD",
  "notes": "Looks good",
  "contactEmail": "jane@example.com",
  "shippingAddress": "123 Main St",
  "billingAddress": "123 Main St"
}
```

---

### 9. Seller: list orders

<div class="endpoint-header">
  <span class="method-badge get">GET</span>
  <span class="endpoint-path">/api/v1/seller/orders</span>
</div>

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/seller/orders?page=1&perPage=20&status=PENDING&q=phone&from=2026-05-01&to=2026-05-31' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <seller_token>'
```

#### Response

```json
{
  "data": [
    {
      "orderId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "orderNumber": "ORD-20240506-0001-A1B2C3D4",
      "userId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "shopId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "subTotal": "250.00",
      "shippingCost": "25.00",
      "taxAmount": "250.00",
      "discountAmount": "250.00",
      "total": "250.00",
      "currency": "USD",
      "status": "PENDING",
      "allowedActions": [
        "APPROVE"
      ],
      "nextStatuses": [
        "SHIPPED"
      ],
      "paymentStatus": "PENDING",
      "couponCode": "SAVE10",
      "paymentMethod": "CREDIT_CARD",
      "notes": "Looks good",
      "contactEmail": "jane@example.com",
      "shippingAddress": "123 Main St",
      "billingAddress": "123 Main St"
    }
  ],
  "metadata": {
    "totalCount": 1,
    "limit": 20,
    "offset": 0,
    "page": 1,
    "perPage": 20,
    "totalPages": 1,
    "hasNext": false,
    "hasPrev": false
  }
}
```

---

### 10. Admin - Update the status of any order

<div class="endpoint-header">
  <span class="method-badge patch">PATCH</span>
  <span class="endpoint-path">/api/v1/admin/orders/status/{id}</span>
</div>

```bash
curl -X 'PATCH' \
  'http://localhost:8080/api/v1/admin/orders/status/6c5078d3-f8e3-4c88-9afe-48b5423c664f?status=PENDING' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <admin_token>'
```

#### Response

```json
{
  "orderId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "orderNumber": "ORD-20240506-0001-A1B2C3D4",
  "userId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "shopId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "subTotal": "250.00",
  "shippingCost": "25.00",
  "taxAmount": "250.00",
  "discountAmount": "250.00",
  "total": "250.00",
  "currency": "USD",
  "status": "PENDING",
  "allowedActions": [
    "APPROVE"
  ],
  "nextStatuses": [
    "SHIPPED"
  ],
  "paymentStatus": "PENDING",
  "couponCode": "SAVE10",
  "paymentMethod": "CREDIT_CARD",
  "notes": "Looks good",
  "contactEmail": "jane@example.com",
  "shippingAddress": "123 Main St",
  "billingAddress": "123 Main St"
}
```

---

### 11. Admin - Cancel any order

<div class="endpoint-header">
  <span class="method-badge post">POST</span>
  <span class="endpoint-path">/api/v1/admin/orders/{id}/cancel</span>
</div>

```bash
curl -X 'POST' \
  'http://localhost:8080/api/v1/admin/orders/6c5078d3-f8e3-4c88-9afe-48b5423c664f/cancel' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <admin_token>' \
  -H 'Content-Type: application/json' \
  -d '{
  "reason": "Policy violation"
}'
```

#### Response

```json
{
  "orderId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "orderNumber": "ORD-20240506-0001-A1B2C3D4",
  "userId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "shopId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "subTotal": "250.00",
  "shippingCost": "25.00",
  "taxAmount": "250.00",
  "discountAmount": "250.00",
  "total": "250.00",
  "currency": "USD",
  "status": "PENDING",
  "allowedActions": [
    "APPROVE"
  ],
  "nextStatuses": [
    "SHIPPED"
  ],
  "paymentStatus": "PENDING",
  "couponCode": "SAVE10",
  "paymentMethod": "CREDIT_CARD",
  "notes": "Looks good",
  "contactEmail": "jane@example.com",
  "shippingAddress": "123 Main St",
  "billingAddress": "123 Main St"
}
```

---

### 12. Printable order invoice

<div class="endpoint-header">
  <span class="method-badge get">GET</span>
  <span class="endpoint-path">/api/v1/orders/{id}/invoice</span>
</div>

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/orders/6c5078d3-f8e3-4c88-9afe-48b5423c664f/invoice' \
  -H 'accept: text/html' \
  -H 'Authorization: Bearer <access_token>'
```

#### Response

```html
<!doctype html><html><body>Printable document</body></html>
```

---

### 13. Printable packing slip

<div class="endpoint-header">
  <span class="method-badge get">GET</span>
  <span class="endpoint-path">/api/v1/orders/{id}/packing-slip</span>
</div>

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/orders/6c5078d3-f8e3-4c88-9afe-48b5423c664f/packing-slip' \
  -H 'accept: text/html' \
  -H 'Authorization: Bearer <access_token>'
```

#### Response

```html
<!doctype html><html><body>Printable document</body></html>
```

---

### 14. Admin: export orders CSV

<div class="endpoint-header">
  <span class="method-badge get">GET</span>
  <span class="endpoint-path">/api/v1/admin/orders/export</span>
</div>

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/admin/orders/export?status=PENDING&q=phone&startDate=2026-05-01&endDate=2026-05-31' \
  -H 'accept: text/csv' \
  -H 'Authorization: Bearer <admin_token>'
```

#### Response

```csv
id,status,amount,currency,createdAt
example-id,PAID,250.00,USD,2024-05-06T12:00:00
```

---

### 15. Admin: get order by ID

<div class="endpoint-header">
  <span class="method-badge get">GET</span>
  <span class="endpoint-path">/api/v1/admin/orders/{id}</span>
</div>

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/admin/orders/6c5078d3-f8e3-4c88-9afe-48b5423c664f' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <admin_token>'
```

#### Response

```json
{
  "orderId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "orderNumber": "ORD-20240506-0001-A1B2C3D4",
  "userId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "shopId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "subTotal": "250.00",
  "shippingCost": "25.00",
  "taxAmount": "250.00",
  "discountAmount": "250.00",
  "total": "250.00",
  "currency": "USD",
  "status": "PENDING",
  "allowedActions": [
    "APPROVE"
  ],
  "nextStatuses": [
    "SHIPPED"
  ],
  "paymentStatus": "PENDING",
  "couponCode": "SAVE10",
  "paymentMethod": "CREDIT_CARD",
  "notes": "Looks good",
  "contactEmail": "jane@example.com",
  "shippingAddress": "123 Main St",
  "billingAddress": "123 Main St"
}
```

---

### 16. Guest order invoice

<div class="endpoint-header">
  <span class="method-badge get">GET</span>
  <span class="endpoint-path">/api/v1/orders/track/invoice</span>
</div>

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/orders/track/invoice?orderNumber=ORD-20240506-0001-A1B2C3D4&email=jane@example.com' \
  -H 'accept: text/html'
```

#### Response

```html
<!doctype html><html><body>Printable document</body></html>
```

---

### 17. Guest order timeline

<div class="endpoint-header">
  <span class="method-badge get">GET</span>
  <span class="endpoint-path">/api/v1/orders/track/timeline</span>
</div>

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/orders/track/timeline?orderNumber=ORD-20240506-0001-A1B2C3D4&email=jane@example.com' \
  -H 'accept: application/json'
```

#### Response

```json
{
  "orderNumber": "ORD-20240506-0001-A1B2C3D4",
  "status": "PENDING",
  "trackingNumber": "1Z999AA10123456784",
  "milestones": [
    {
      "key": "pending-orders",
      "label": "Pending orders",
      "occurredAt": "2024-05-06T12:00:00"
    }
  ]
}
```

---

### 18. Get order by ID

<div class="endpoint-header">
  <span class="method-badge get">GET</span>
  <span class="endpoint-path">/api/v1/orders/{id}</span>
</div>

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/orders/6c5078d3-f8e3-4c88-9afe-48b5423c664f' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <access_token>'
```

#### Response

```json
{
  "orderId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "orderNumber": "ORD-20240506-0001-A1B2C3D4",
  "userId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "shopId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "subTotal": "250.00",
  "shippingCost": "25.00",
  "taxAmount": "250.00",
  "discountAmount": "250.00",
  "total": "250.00",
  "currency": "USD",
  "status": "PENDING",
  "allowedActions": [
    "APPROVE"
  ],
  "nextStatuses": [
    "SHIPPED"
  ],
  "paymentStatus": "PENDING",
  "couponCode": "SAVE10",
  "paymentMethod": "CREDIT_CARD",
  "notes": "Looks good",
  "contactEmail": "jane@example.com",
  "shippingAddress": "123 Main St",
  "billingAddress": "123 Main St"
}
```

---

### 19. List order shipments

<div class="endpoint-header">
  <span class="method-badge get">GET</span>
  <span class="endpoint-path">/api/v1/orders/{id}/shipments</span>
</div>

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/orders/6c5078d3-f8e3-4c88-9afe-48b5423c664f/shipments' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <access_token>'
```

#### Response

```json
[
  {
    "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
    "orderId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
    "carrier": "string",
    "service": "string",
    "trackingNumber": "1Z999AA10123456784",
    "labelUrl": "string",
    "rateAmount": "250.00",
    "currency": "USD",
    "status": "PENDING",
    "shippedAt": "2024-05-06T12:00:00",
    "deliveredAt": "2024-05-06T12:00:00",
    "createdAt": "2024-05-06T12:00:00",
    "items": [
      {
        "orderItemId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
        "quantity": 1
      }
    ]
  }
]
```

---

### 20. Purchase a shipping label

<div class="endpoint-header">
  <span class="method-badge post">POST</span>
  <span class="endpoint-path">/api/v1/orders/{id}/shipments</span>
</div>

```bash
curl -X 'POST' \
  'http://localhost:8080/api/v1/orders/6c5078d3-f8e3-4c88-9afe-48b5423c664f/shipments' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <access_token>' \
  -H 'Content-Type: application/json' \
  -d '{
  "rateId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "items": [
    {
      "orderItemId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "quantity": 1
    }
  ],
  "idempotencyKey": "idem-123456"
}'
```

#### Response

```json
{
  "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "orderId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "carrier": "string",
  "service": "string",
  "trackingNumber": "1Z999AA10123456784",
  "labelUrl": "string",
  "rateAmount": "250.00",
  "currency": "USD",
  "status": "PENDING",
  "shippedAt": "2024-05-06T12:00:00",
  "deliveredAt": "2024-05-06T12:00:00",
  "createdAt": "2024-05-06T12:00:00",
  "items": [
    {
      "orderItemId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "quantity": 1
    }
  ]
}
```

---

### 21. Get shipping rate quotes

<div class="endpoint-header">
  <span class="method-badge post">POST</span>
  <span class="endpoint-path">/api/v1/orders/{id}/shipping-quotes</span>
</div>

```bash
curl -X 'POST' \
  'http://localhost:8080/api/v1/orders/6c5078d3-f8e3-4c88-9afe-48b5423c664f/shipping-quotes' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <access_token>' \
  -H 'Content-Type: application/json' \
  -d '{
  "origin": {
    "name": "Example",
    "streetAddress": "123 Main St",
    "city": "New York",
    "state": "PENDING",
    "zipCode": "SAVE10",
    "country": "US",
    "phone": "+15551234567",
    "email": "jane@example.com"
  },
  "destination": {
    "name": "Example",
    "streetAddress": "123 Main St",
    "city": "New York",
    "state": "PENDING",
    "zipCode": "SAVE10",
    "country": "US",
    "phone": "+15551234567",
    "email": "jane@example.com"
  },
  "parcels": [
    {
      "length": "string",
      "width": "string",
      "height": "string",
      "distanceUnit": "string",
      "weight": "string",
      "massUnit": "string"
    }
  ]
}'
```

#### Response

```json
[
  {
    "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
    "carrier": "string",
    "service": "string",
    "amount": "250.00",
    "currency": "USD",
    "estimatedDays": 1
  }
]
```

---

### 22. Seller: export orders CSV

<div class="endpoint-header">
  <span class="method-badge get">GET</span>
  <span class="endpoint-path">/api/v1/seller/orders/export</span>
</div>

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/seller/orders/export?status=PENDING&q=phone&from=2026-05-01&to=2026-05-31' \
  -H 'accept: text/csv' \
  -H 'Authorization: Bearer <seller_token>'
```

#### Response

```csv
id,status,amount,currency,createdAt
example-id,PAID,250.00,USD,2024-05-06T12:00:00
```

---

### 23. Seller: get order by ID

<div class="endpoint-header">
  <span class="method-badge get">GET</span>
  <span class="endpoint-path">/api/v1/seller/orders/{id}</span>
</div>

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/seller/orders/6c5078d3-f8e3-4c88-9afe-48b5423c664f' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <seller_token>'
```

#### Response

```json
{
  "orderId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "orderNumber": "ORD-20240506-0001-A1B2C3D4",
  "userId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "shopId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "subTotal": "250.00",
  "shippingCost": "25.00",
  "taxAmount": "250.00",
  "discountAmount": "250.00",
  "total": "250.00",
  "currency": "USD",
  "status": "PENDING",
  "allowedActions": [
    "APPROVE"
  ],
  "nextStatuses": [
    "SHIPPED"
  ],
  "paymentStatus": "PENDING",
  "couponCode": "SAVE10",
  "paymentMethod": "CREDIT_CARD",
  "notes": "Looks good",
  "contactEmail": "jane@example.com",
  "shippingAddress": "123 Main St",
  "billingAddress": "123 Main St"
}
```

---

### 24. Seller: cancel order

<div class="endpoint-header">
  <span class="method-badge post">POST</span>
  <span class="endpoint-path">/api/v1/seller/orders/{id}/cancel</span>
</div>

```bash
curl -X 'POST' \
  'http://localhost:8080/api/v1/seller/orders/6c5078d3-f8e3-4c88-9afe-48b5423c664f/cancel' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <seller_token>' \
  -H 'Content-Type: application/json' \
  -d '{
  "reason": "Policy violation"
}'
```

#### Response

```json
{
  "orderId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "orderNumber": "ORD-20240506-0001-A1B2C3D4",
  "userId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "shopId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "subTotal": "250.00",
  "shippingCost": "25.00",
  "taxAmount": "250.00",
  "discountAmount": "250.00",
  "total": "250.00",
  "currency": "USD",
  "status": "PENDING",
  "allowedActions": [
    "APPROVE"
  ],
  "nextStatuses": [
    "SHIPPED"
  ],
  "paymentStatus": "PENDING",
  "couponCode": "SAVE10",
  "paymentMethod": "CREDIT_CARD",
  "notes": "Looks good",
  "contactEmail": "jane@example.com",
  "shippingAddress": "123 Main St",
  "billingAddress": "123 Main St"
}
```

---

### 25. Seller: verify pickup code and complete collection

<div class="endpoint-header">
  <span class="method-badge post">POST</span>
  <span class="endpoint-path">/api/v1/seller/orders/{id}/pickup/complete</span>
</div>

```bash
curl -X 'POST' \
  'http://localhost:8080/api/v1/seller/orders/6c5078d3-f8e3-4c88-9afe-48b5423c664f/pickup/complete' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <seller_token>' \
  -H 'Content-Type: application/json' \
  -d '{
  "pickupCode": "SAVE10"
}'
```

#### Response

```json
{
  "orderId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "orderNumber": "ORD-20240506-0001-A1B2C3D4",
  "userId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "shopId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "subTotal": "250.00",
  "shippingCost": "25.00",
  "taxAmount": "250.00",
  "discountAmount": "250.00",
  "total": "250.00",
  "currency": "USD",
  "status": "PENDING",
  "allowedActions": [
    "APPROVE"
  ],
  "nextStatuses": [
    "SHIPPED"
  ],
  "paymentStatus": "PENDING",
  "couponCode": "SAVE10",
  "paymentMethod": "CREDIT_CARD",
  "notes": "Looks good",
  "contactEmail": "jane@example.com",
  "shippingAddress": "123 Main St",
  "billingAddress": "123 Main St"
}
```

---

### 26. Seller: mark a pickup order ready

<div class="endpoint-header">
  <span class="method-badge post">POST</span>
  <span class="endpoint-path">/api/v1/seller/orders/{id}/pickup/ready</span>
</div>

```bash
curl -X 'POST' \
  'http://localhost:8080/api/v1/seller/orders/6c5078d3-f8e3-4c88-9afe-48b5423c664f/pickup/ready' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <seller_token>'
```

#### Response

```json
{
  "orderId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "orderNumber": "ORD-20240506-0001-A1B2C3D4",
  "userId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "shopId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "subTotal": "250.00",
  "shippingCost": "25.00",
  "taxAmount": "250.00",
  "discountAmount": "250.00",
  "total": "250.00",
  "currency": "USD",
  "status": "PENDING",
  "allowedActions": [
    "APPROVE"
  ],
  "nextStatuses": [
    "SHIPPED"
  ],
  "paymentStatus": "PENDING",
  "couponCode": "SAVE10",
  "paymentMethod": "CREDIT_CARD",
  "notes": "Looks good",
  "contactEmail": "jane@example.com",
  "shippingAddress": "123 Main St",
  "billingAddress": "123 Main St"
}
```

---

### 27. Seller: mark order shipped

<div class="endpoint-header">
  <span class="method-badge post">POST</span>
  <span class="endpoint-path">/api/v1/seller/orders/{id}/shipping</span>
</div>

```bash
curl -X 'POST' \
  'http://localhost:8080/api/v1/seller/orders/6c5078d3-f8e3-4c88-9afe-48b5423c664f/shipping' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <seller_token>' \
  -H 'Content-Type: application/json' \
  -d '{
  "trackingNumber": "1Z999AA10123456784"
}'
```

#### Response

```json
{
  "orderId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "orderNumber": "ORD-20240506-0001-A1B2C3D4",
  "userId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "shopId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "subTotal": "250.00",
  "shippingCost": "25.00",
  "taxAmount": "250.00",
  "discountAmount": "250.00",
  "total": "250.00",
  "currency": "USD",
  "status": "PENDING",
  "allowedActions": [
    "APPROVE"
  ],
  "nextStatuses": [
    "SHIPPED"
  ],
  "paymentStatus": "PENDING",
  "couponCode": "SAVE10",
  "paymentMethod": "CREDIT_CARD",
  "notes": "Looks good",
  "contactEmail": "jane@example.com",
  "shippingAddress": "123 Main St",
  "billingAddress": "123 Main St"
}
```

---

## Error Handling

| Status Code | Description |
|-------------|-------------|
| `400` | Bad Request (Invalid status transition) |
| `401` | Unauthorized |
| `403` | Forbidden (User does not own the order) |
| `404` | Not Found |
