# RMA / Returns & Exchanges

Return merchandise authorization (RMA) covers physical returns and exchanges for delivered, paid order items.

## Customer endpoints

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/v1/rma-requests` | Create a return/exchange request |
| `GET` | `/api/v1/rma-requests` | List your RMA requests |
| `POST` | `/api/v1/rma-requests/{id}/ship` | Mark an approved RMA as shipped back |
| `POST` | `/api/v1/rma-requests/{id}/cancel` | Cancel a pending/approved RMA |

Create body:

```json
{
  "orderId": "order-uuid",
  "orderItemId": "item-uuid",
  "quantity": 1,
  "type": "RETURN",
  "reason": "Item arrived damaged"
}
```

## Seller/Admin endpoints

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/v1/seller/rma-requests` | Seller queue |
| `PUT` | `/api/v1/seller/rma-requests/{id}/status` | Approve/reject/receive/complete |
| `GET` | `/api/v1/admin/rma-requests` | Admin queue (optional `status`) |
| `PUT` | `/api/v1/admin/rma-requests/{id}/status` | Admin status update |
| `POST` | `/api/v1/admin/rma-requests/bulk/status` | Admin: bulk update RMA status |
| `GET` | `/api/v1/admin/rma-requests/export` | Admin: export RMAs CSV |

Status update body:

```json
{
  "status": "COMPLETED",
  "resolution": "REFUND",
  "replacementOrderId": null,
  "note": "Returned item received"
}
```

- `REFUND` creates/links a normal refund request for the returned quantity.
- `REPLACEMENT` requires `replacementOrderId` belonging to the same customer and shop; returned stock is restored.

## Lifecycle

`REQUESTED → APPROVED → IN_TRANSIT → RECEIVED → COMPLETED`, or `REQUESTED/APPROVED → CANCELLED`, or `REQUESTED → REJECTED`.
## Endpoint Details

### 1. Create return/exchange request

**`POST /api/v1/rma-requests`**

Create return/exchange request

#### Request

```bash
curl -X 'POST' \
  'http://localhost:8080/api/v1/rma-requests' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <access_token>' \
  -H 'Content-Type: application/json' \
  -d '{
  "orderId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "orderItemId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "quantity": 1,
  "type": "RETURN",
  "reason": "Policy violation"
}'
```

#### Response

```json
{
  "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "orderId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "orderItemId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "userId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "shopId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "quantity": 1,
  "type": "RETURN",
  "status": "APPROVED",
  "reason": "Policy violation",
  "resolution": "REFUND",
  "note": "string",
  "orderNumber": "ORD-20240506-0001-A1B2C3D4",
  "customerEmail": "jane@example.com",
  "productName": "Wireless Headphones",
  "allowedActions": [
    "APPROVE"
  ],
  "trackingNumber": "1Z999AA10123456784",
  "replacementOrderId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "refundRequestId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "createdAt": "2024-05-06T12:00:00",
  "resolvedAt": "2024-05-06T12:00:00"
}
```

---

### 2. List my RMAs

**`GET /api/v1/rma-requests`**

List my RMAs

#### Request

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/rma-requests?page=1&perPage=20' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <access_token>'
```

#### Response

```json
{
  "data": [
    {
      "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "orderId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "orderItemId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "userId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "shopId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "quantity": 1,
      "type": "RETURN",
      "status": "APPROVED",
      "reason": "Policy violation",
      "resolution": "REFUND",
      "note": "string",
      "orderNumber": "ORD-20240506-0001-A1B2C3D4",
      "customerEmail": "jane@example.com",
      "productName": "Wireless Headphones",
      "allowedActions": [
        "APPROVE"
      ],
      "trackingNumber": "1Z999AA10123456784",
      "replacementOrderId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "refundRequestId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "createdAt": "2024-05-06T12:00:00",
      "resolvedAt": "2024-05-06T12:00:00"
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

### 3. Mark RMA as shipped

**`POST /api/v1/rma-requests/{id}/ship`**

Mark RMA as shipped

#### Request

```bash
curl -X 'POST' \
  'http://localhost:8080/api/v1/rma-requests/6c5078d3-f8e3-4c88-9afe-48b5423c664f/ship' \
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
  "orderId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "orderItemId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "userId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "shopId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "quantity": 1,
  "type": "RETURN",
  "status": "APPROVED",
  "reason": "Policy violation",
  "resolution": "REFUND",
  "note": "string",
  "orderNumber": "ORD-20240506-0001-A1B2C3D4",
  "customerEmail": "jane@example.com",
  "productName": "Wireless Headphones",
  "allowedActions": [
    "APPROVE"
  ],
  "trackingNumber": "1Z999AA10123456784",
  "replacementOrderId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "refundRequestId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "createdAt": "2024-05-06T12:00:00",
  "resolvedAt": "2024-05-06T12:00:00"
}
```

---

### 4. Cancel RMA

**`POST /api/v1/rma-requests/{id}/cancel`**

Cancel RMA

#### Request

```bash
curl -X 'POST' \
  'http://localhost:8080/api/v1/rma-requests/6c5078d3-f8e3-4c88-9afe-48b5423c664f/cancel' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <access_token>'
```

#### Response

```json
{
  "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "orderId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "orderItemId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "userId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "shopId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "quantity": 1,
  "type": "RETURN",
  "status": "APPROVED",
  "reason": "Policy violation",
  "resolution": "REFUND",
  "note": "string",
  "orderNumber": "ORD-20240506-0001-A1B2C3D4",
  "customerEmail": "jane@example.com",
  "productName": "Wireless Headphones",
  "allowedActions": [
    "APPROVE"
  ],
  "trackingNumber": "1Z999AA10123456784",
  "replacementOrderId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "refundRequestId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "createdAt": "2024-05-06T12:00:00",
  "resolvedAt": "2024-05-06T12:00:00"
}
```

---
### 5. Seller: list RMAs

**`GET /api/v1/seller/rma-requests`**

Seller: list RMAs

#### Request

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/seller/rma-requests?page=1&perPage=20&status=PENDING&q=phone&from=2026-05-01&to=2026-05-31' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <seller_token>'
```

#### Response

```json
{
  "data": [
    {
      "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "orderId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "orderItemId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "userId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "shopId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "quantity": 1,
      "type": "RETURN",
      "status": "APPROVED",
      "reason": "Policy violation",
      "resolution": "REFUND",
      "note": "string",
      "orderNumber": "ORD-20240506-0001-A1B2C3D4",
      "customerEmail": "jane@example.com",
      "productName": "Wireless Headphones",
      "allowedActions": [
        "APPROVE"
      ],
      "trackingNumber": "1Z999AA10123456784",
      "replacementOrderId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "refundRequestId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "createdAt": "2024-05-06T12:00:00",
      "resolvedAt": "2024-05-06T12:00:00"
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

### 6. Seller: update RMA status

**`PUT /api/v1/seller/rma-requests/{id}/status`**

Seller: update RMA status

#### Request

```bash
curl -X 'PUT' \
  'http://localhost:8080/api/v1/seller/rma-requests/6c5078d3-f8e3-4c88-9afe-48b5423c664f/status' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <seller_token>' \
  -H 'Content-Type: application/json' \
  -d '{
  "status": "APPROVED",
  "resolution": "REFUND",
  "replacementOrderId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "note": "string"
}'
```

#### Response

```json
{
  "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "orderId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "orderItemId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "userId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "shopId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "quantity": 1,
  "type": "RETURN",
  "status": "APPROVED",
  "reason": "Policy violation",
  "resolution": "REFUND",
  "note": "string",
  "orderNumber": "ORD-20240506-0001-A1B2C3D4",
  "customerEmail": "jane@example.com",
  "productName": "Wireless Headphones",
  "allowedActions": [
    "APPROVE"
  ],
  "trackingNumber": "1Z999AA10123456784",
  "replacementOrderId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "refundRequestId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "createdAt": "2024-05-06T12:00:00",
  "resolvedAt": "2024-05-06T12:00:00"
}
```

---

### 7. Admin: list RMAs

**`GET /api/v1/admin/rma-requests`**

Admin: list RMAs

#### Request

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/admin/rma-requests?page=1&perPage=20&status=PENDING&q=phone&from=2026-05-01&to=2026-05-31' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <admin_token>'
```

#### Response

```json
{
  "data": [
    {
      "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "orderId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "orderItemId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "userId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "shopId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "quantity": 1,
      "type": "RETURN",
      "status": "APPROVED",
      "reason": "Policy violation",
      "resolution": "REFUND",
      "note": "string",
      "orderNumber": "ORD-20240506-0001-A1B2C3D4",
      "customerEmail": "jane@example.com",
      "productName": "Wireless Headphones",
      "allowedActions": [
        "APPROVE"
      ],
      "trackingNumber": "1Z999AA10123456784",
      "replacementOrderId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "refundRequestId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "createdAt": "2024-05-06T12:00:00",
      "resolvedAt": "2024-05-06T12:00:00"
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

### 8. Admin: update RMA status

**`PUT /api/v1/admin/rma-requests/{id}/status`**

Admin: update RMA status

#### Request

```bash
curl -X 'PUT' \
  'http://localhost:8080/api/v1/admin/rma-requests/6c5078d3-f8e3-4c88-9afe-48b5423c664f/status' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <admin_token>' \
  -H 'Content-Type: application/json' \
  -d '{
  "status": "APPROVED",
  "resolution": "REFUND",
  "replacementOrderId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "note": "string"
}'
```

#### Response

```json
{
  "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "orderId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "orderItemId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "userId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "shopId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "quantity": 1,
  "type": "RETURN",
  "status": "APPROVED",
  "reason": "Policy violation",
  "resolution": "REFUND",
  "note": "string",
  "orderNumber": "ORD-20240506-0001-A1B2C3D4",
  "customerEmail": "jane@example.com",
  "productName": "Wireless Headphones",
  "allowedActions": [
    "APPROVE"
  ],
  "trackingNumber": "1Z999AA10123456784",
  "replacementOrderId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "refundRequestId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "createdAt": "2024-05-06T12:00:00",
  "resolvedAt": "2024-05-06T12:00:00"
}
```

---

### 9. Admin: bulk update RMA status

**`POST /api/v1/admin/rma-requests/bulk/status`**

Admin: bulk update RMA status

#### Request

```bash
curl -X 'POST' \
  'http://localhost:8080/api/v1/admin/rma-requests/bulk/status?status=PENDING' \
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

### 10. Admin: export RMAs CSV

**`GET /api/v1/admin/rma-requests/export`**

Admin: export RMAs CSV

#### Request

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/admin/rma-requests/export?status=PENDING&q=phone&from=2026-05-01&to=2026-05-31' \
  -H 'accept: text/csv' \
  -H 'Authorization: Bearer <admin_token>'
```

#### Response

```csv
id,status,amount,currency,createdAt
example-id,PAID,250.00,USD,2024-05-06T12:00:00
```

---
