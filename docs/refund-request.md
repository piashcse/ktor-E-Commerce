# Refund Request

Customer refund requests, evidence upload and seller/admin decisions.

## Authentication

All Refund Request endpoints require Bearer token authentication.

### Refund Request Endpoints

| Method | Endpoint | Description | Authentication Required |
|--------|----------|-------------|------------------------|
| <span class="method-badge post">POST</span> | `/api/v1/refund-requests/{orderId}` | Create a refund request (supports `quantity`, shipping/tax refund fields) | Yes (Customer) |
| <span class="method-badge get">GET</span> | `/api/v1/refund-requests/{id}` | Get refund request details | Yes |
| <span class="method-badge get">GET</span> | `/api/v1/refund-requests/order/{orderId}` | Get refund requests for an order | Yes (Customer) |
| <span class="method-badge get">GET</span> | `/api/v1/refund-requests/{id}/evidence/{fileName}` | Get private refund evidence for an authorized refund request | Yes |
| <span class="method-badge put">PUT</span> | `/api/v1/seller/refund-requests/{id}/status` | Update refund status (Seller) | Yes (Seller) |
| <span class="method-badge post">POST</span> | `/api/v1/refund-requests/{id}/ship` | Mark refund as shipped | Yes (Customer) |
| <span class="method-badge get">GET</span> | `/api/v1/admin/refund-requests/order/{orderId}` | Get refunds for an order (Admin) | Yes (Admin) |
| <span class="method-badge put">PUT</span> | `/api/v1/admin/refund-requests/{id}/status` | Update refund status (Admin) | Yes (Admin) |
| <span class="method-badge get">GET</span> | `/api/v1/admin/refund-requests` | Admin - List refund requests | Yes |
| <span class="method-badge post">POST</span> | `/api/v1/admin/refund-requests/bulk/status` | Admin - Bulk update refund status | Yes |
| <span class="method-badge get">GET</span> | `/api/v1/admin/refund-requests/export` | Admin - Export refunds CSV | Yes |
| <span class="method-badge post">POST</span> | `/api/v1/refund-requests/{id}/evidence` | Upload private evidence for a refund request | Yes |
| <span class="method-badge get">GET</span> | `/api/v1/seller/refund-requests` | Seller - List refund requests | Yes |

---

## Endpoint Details

### 1. Refund evidence

<div class="endpoint-header">
  <span class="method-badge get">GET</span>
  <span class="endpoint-path">/api/v1/refund-requests/{id}/evidence/{fileName}</span>
</div>

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/refund-requests/6c5078d3-f8e3-4c88-9afe-48b5423c664f/evidence/receipt.png' \
  -H 'Authorization: Bearer <customer_or_admin_token>' \
  --output receipt.png
```

### 2. Update Refund Status (Seller)

<div class="endpoint-header">
  <span class="method-badge put">PUT</span>
  <span class="endpoint-path">/api/v1/seller/refund-requests/{id}/status</span>
</div>

#### Request

```bash
curl -X 'PUT' \
  'http://localhost:8080/api/v1/seller/refund-requests/refund-uuid-123/status' \
  -H 'Authorization: Bearer <seller_token>' \
  -H 'Content-Type: application/json' \
  -d '{
  "status": "APPROVED",
  "refundAmount": 50.0,
  "refundShippingAmount": 5.0,
  "refundTaxAmount": 2.5,
  "refundMethod": "ORIGINAL"
}'
```

---

### 3. Create a refund request for an order item

<div class="endpoint-header">
  <span class="method-badge post">POST</span>
  <span class="endpoint-path">/api/v1/refund-requests/{orderId}</span>
</div>

```bash
curl -X 'POST' \
  'http://localhost:8080/api/v1/refund-requests/ORD-20240506-0001-A1B2C3D4' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <access_token>' \
  -H 'Content-Type: application/json' \
  -d '{
  "orderItemId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "reason": "Policy violation",
  "images": "https://example.com/image.jpg",
  "quantity": 1
}'
```

#### Response

```json
{
  "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "orderItemId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "orderId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "orderNumber": "ORD-20240506-0001-A1B2C3D4",
  "shopId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "customerEmail": "jane@example.com",
  "productName": "Wireless Headphones",
  "userId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "reason": "Policy violation",
  "images": "https://example.com/image.jpg",
  "decisionReason": "Policy violation",
  "status": "PENDING",
  "currency": "USD",
  "quantity": 1,
  "refundAmount": "250.00",
  "shippingRefund": "25.00",
  "taxRefund": "25.00",
  "refundMethod": "ORIGINAL",
  "trackingNumber": "1Z999AA10123456784",
  "allowedActions": [
    "APPROVE"
  ]
}
```

---

### 4. Get refund request details

<div class="endpoint-header">
  <span class="method-badge get">GET</span>
  <span class="endpoint-path">/api/v1/refund-requests/{id}</span>
</div>

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/refund-requests/6c5078d3-f8e3-4c88-9afe-48b5423c664f' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <access_token>'
```

#### Response

```json
{
  "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "orderItemId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "orderId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "orderNumber": "ORD-20240506-0001-A1B2C3D4",
  "shopId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "customerEmail": "jane@example.com",
  "productName": "Wireless Headphones",
  "userId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "reason": "Policy violation",
  "images": "https://example.com/image.jpg",
  "decisionReason": "Policy violation",
  "status": "PENDING",
  "currency": "USD",
  "quantity": 1,
  "refundAmount": "250.00",
  "shippingRefund": "25.00",
  "taxRefund": "25.00",
  "refundMethod": "ORIGINAL",
  "trackingNumber": "1Z999AA10123456784",
  "allowedActions": [
    "APPROVE"
  ]
}
```

---

### 5. Get refund requests for an order

<div class="endpoint-header">
  <span class="method-badge get">GET</span>
  <span class="endpoint-path">/api/v1/refund-requests/order/{orderId}</span>
</div>

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/refund-requests/order/ORD-20240506-0001-A1B2C3D4?page=1&perPage=20' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <access_token>'
```

#### Response

```json
{
  "data": [
    {
      "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "orderItemId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "orderId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "orderNumber": "ORD-20240506-0001-A1B2C3D4",
      "shopId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "customerEmail": "jane@example.com",
      "productName": "Wireless Headphones",
      "userId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "reason": "Policy violation",
      "images": "https://example.com/image.jpg",
      "decisionReason": "Policy violation",
      "status": "PENDING",
      "currency": "USD",
      "quantity": 1,
      "refundAmount": "250.00",
      "shippingRefund": "25.00",
      "taxRefund": "25.00",
      "refundMethod": "ORIGINAL",
      "trackingNumber": "1Z999AA10123456784",
      "allowedActions": [
        "APPROVE"
      ]
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

### 6. Mark an approved refund as shipped

<div class="endpoint-header">
  <span class="method-badge post">POST</span>
  <span class="endpoint-path">/api/v1/refund-requests/{id}/ship</span>
</div>

```bash
curl -X 'POST' \
  'http://localhost:8080/api/v1/refund-requests/6c5078d3-f8e3-4c88-9afe-48b5423c664f/ship' \
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
  "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "orderItemId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "orderId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "orderNumber": "ORD-20240506-0001-A1B2C3D4",
  "shopId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "customerEmail": "jane@example.com",
  "productName": "Wireless Headphones",
  "userId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "reason": "Policy violation",
  "images": "https://example.com/image.jpg",
  "decisionReason": "Policy violation",
  "status": "PENDING",
  "currency": "USD",
  "quantity": 1,
  "refundAmount": "250.00",
  "shippingRefund": "25.00",
  "taxRefund": "25.00",
  "refundMethod": "ORIGINAL",
  "trackingNumber": "1Z999AA10123456784",
  "allowedActions": [
    "APPROVE"
  ]
}
```

---

### 7. Admin - Get all refund requests for an order

<div class="endpoint-header">
  <span class="method-badge get">GET</span>
  <span class="endpoint-path">/api/v1/admin/refund-requests/order/{orderId}</span>
</div>

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/admin/refund-requests/order/ORD-20240506-0001-A1B2C3D4?page=1&perPage=20' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <admin_token>'
```

#### Response

```json
{
  "data": [
    {
      "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "orderItemId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "orderId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "orderNumber": "ORD-20240506-0001-A1B2C3D4",
      "shopId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "customerEmail": "jane@example.com",
      "productName": "Wireless Headphones",
      "userId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "reason": "Policy violation",
      "images": "https://example.com/image.jpg",
      "decisionReason": "Policy violation",
      "status": "PENDING",
      "currency": "USD",
      "quantity": 1,
      "refundAmount": "250.00",
      "shippingRefund": "25.00",
      "taxRefund": "25.00",
      "refundMethod": "ORIGINAL",
      "trackingNumber": "1Z999AA10123456784",
      "allowedActions": [
        "APPROVE"
      ]
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

### 8. Admin - Update refund status

<div class="endpoint-header">
  <span class="method-badge put">PUT</span>
  <span class="endpoint-path">/api/v1/admin/refund-requests/{id}/status</span>
</div>

```bash
curl -X 'PUT' \
  'http://localhost:8080/api/v1/admin/refund-requests/6c5078d3-f8e3-4c88-9afe-48b5423c664f/status' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <admin_token>' \
  -H 'Content-Type: application/json' \
  -d '{
  "status": "PENDING",
  "refundAmount": "250.00",
  "refundShippingAmount": "250.00",
  "refundTaxAmount": "250.00",
  "refundMethod": "ORIGINAL",
  "decisionReason": "Policy violation"
}'
```

#### Response

```json
{
  "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "orderItemId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "orderId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "orderNumber": "ORD-20240506-0001-A1B2C3D4",
  "shopId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "customerEmail": "jane@example.com",
  "productName": "Wireless Headphones",
  "userId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "reason": "Policy violation",
  "images": "https://example.com/image.jpg",
  "decisionReason": "Policy violation",
  "status": "PENDING",
  "currency": "USD",
  "quantity": 1,
  "refundAmount": "250.00",
  "shippingRefund": "25.00",
  "taxRefund": "25.00",
  "refundMethod": "ORIGINAL",
  "trackingNumber": "1Z999AA10123456784",
  "allowedActions": [
    "APPROVE"
  ]
}
```

---

### 9. Admin - List refund requests

<div class="endpoint-header">
  <span class="method-badge get">GET</span>
  <span class="endpoint-path">/api/v1/admin/refund-requests</span>
</div>

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/admin/refund-requests?page=1&perPage=20&status=PENDING&q=phone&from=2026-05-01&to=2026-05-31' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <admin_token>'
```

#### Response

```json
{
  "data": [
    {
      "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "orderItemId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "orderId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "orderNumber": "ORD-20240506-0001-A1B2C3D4",
      "shopId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "customerEmail": "jane@example.com",
      "productName": "Wireless Headphones",
      "userId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "reason": "Policy violation",
      "images": "https://example.com/image.jpg",
      "decisionReason": "Policy violation",
      "status": "PENDING",
      "currency": "USD",
      "quantity": 1,
      "refundAmount": "250.00",
      "shippingRefund": "25.00",
      "taxRefund": "25.00",
      "refundMethod": "ORIGINAL",
      "trackingNumber": "1Z999AA10123456784",
      "allowedActions": [
        "APPROVE"
      ]
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

### 10. Admin - Bulk update refund status

<div class="endpoint-header">
  <span class="method-badge post">POST</span>
  <span class="endpoint-path">/api/v1/admin/refund-requests/bulk/status</span>
</div>

```bash
curl -X 'POST' \
  'http://localhost:8080/api/v1/admin/refund-requests/bulk/status?status=PENDING' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <admin_token>' \
  -H 'Content-Type: application/json' \
  -d '{
  "ids": [
    "6c5078d3-f8e3-4c88-9afe-48b5423c664f"
  ],
  "reason": "Policy violation"
}'
```

#### Response

```json
{
  "requested": 1,
  "succeeded": 1,
  "failed": []
}
```

---

### 11. Admin - Export refunds CSV

<div class="endpoint-header">
  <span class="method-badge get">GET</span>
  <span class="endpoint-path">/api/v1/admin/refund-requests/export</span>
</div>

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/admin/refund-requests/export?status=PENDING&q=phone&from=2026-05-01&to=2026-05-31' \
  -H 'accept: text/csv' \
  -H 'Authorization: Bearer <admin_token>'
```

#### Response

```csv
id,status,amount,currency,createdAt
example-id,PAID,250.00,USD,2024-05-06T12:00:00
```

---

### 12. Upload private evidence for a refund request

<div class="endpoint-header">
  <span class="method-badge post">POST</span>
  <span class="endpoint-path">/api/v1/refund-requests/{id}/evidence</span>
</div>

#### Multipart Form Data

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `image` | file | Yes | `image/jpeg`, `image/png` or `image/webp` (`.jpg`, `.jpeg`, `.png`, `.webp`). Maximum **5 MB**. |

#### Headers

| Header | Value | Required |
|--------|-------|----------|
| `Authorization` | `Bearer <access_token>` | Yes |
| `Content-Type` | `multipart/form-data` | Yes |

#### Request

```bash
curl -X 'POST' \
  'http://localhost:8080/api/v1/refund-requests/{id}/evidence' \
  -H 'Authorization: Bearer <access_token>' \
  -H 'Content-Type: multipart/form-data' \
  -F 'image=@evidence.png;type=image/png'
```

#### Response

```json
{
  "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "orderItemId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "orderId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "orderNumber": "ORD-20240506-0001-A1B2C3D4",
  "shopId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "customerEmail": "jane@example.com",
  "productName": "Wireless Headphones",
  "userId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "reason": "Policy violation",
  "images": "https://example.com/image.jpg",
  "decisionReason": "Policy violation",
  "status": "PENDING",
  "currency": "USD",
  "quantity": 1,
  "refundAmount": "250.00",
  "shippingRefund": "25.00",
  "taxRefund": "25.00",
  "refundMethod": "ORIGINAL",
  "trackingNumber": "1Z999AA10123456784",
  "allowedActions": [
    "APPROVE"
  ]
}
```

---

### 13. Seller - List refund requests

<div class="endpoint-header">
  <span class="method-badge get">GET</span>
  <span class="endpoint-path">/api/v1/seller/refund-requests</span>
</div>

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/seller/refund-requests?page=1&perPage=20&status=PENDING&q=phone&from=2026-05-01&to=2026-05-31' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <seller_token>'
```

#### Response

```json
{
  "data": [
    {
      "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "orderItemId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "orderId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "orderNumber": "ORD-20240506-0001-A1B2C3D4",
      "shopId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "customerEmail": "jane@example.com",
      "productName": "Wireless Headphones",
      "userId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "reason": "Policy violation",
      "images": "https://example.com/image.jpg",
      "decisionReason": "Policy violation",
      "status": "PENDING",
      "currency": "USD",
      "quantity": 1,
      "refundAmount": "250.00",
      "shippingRefund": "25.00",
      "taxRefund": "25.00",
      "refundMethod": "ORIGINAL",
      "trackingNumber": "1Z999AA10123456784",
      "allowedActions": [
        "APPROVE"
      ]
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

## Error Handling

| Status Code | Description |
|-------------|-------------|
| `400` | Bad Request |
| `401` | Unauthorized |
| `403` | Forbidden (User does not own the order/shop) |
| `404` | Not Found |
