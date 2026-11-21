# Seller Payouts & Statements

Payouts require an APPROVED shop and VERIFIED seller KYC. Amounts are net of platform
commission. Payout requests are idempotent per seller via `Idempotency-Key`.

## Seller endpoints

| Method | Endpoint | Description | Authentication Required |
|---|---|---|---|
| `GET` | `/api/v1/seller/payouts` | Payout ledger rows | Yes (Seller) |
| `GET` | `/api/v1/seller/payouts/balance` | Available and pending amounts | Yes (Seller) |
| `POST` | `/api/v1/seller/payouts/request` | Request a payout | Yes (Seller) |
| `GET` | `/api/v1/seller/payouts/statement?from=&to=` | Sales/commission/payout/tax statement | Yes (Seller) |
| `GET` | `/api/v1/seller/payouts/statement/export?from=&to=` | Statement CSV export | Yes (Seller) |
| `GET` | `/api/v1/seller/payouts/requests` | Seller: list payout requests | Yes |
| `GET` | `/api/v1/seller/payouts/requests/{id}` | Seller: payout request detail | Yes |

## Admin endpoints

| Method | Endpoint | Description | Authentication Required |
|---|---|---|---|
| `GET` | `/api/v1/admin/payouts` | List all payout requests (optional `status` filter) | Yes (Admin) |
| `GET` | `/api/v1/admin/payouts/export` | Export payouts as CSV | Yes (Admin) |
| `POST` | `/api/v1/admin/payouts/{id}/approve` | Approve a requested payout | Yes (Admin) |
| `POST` | `/api/v1/admin/payouts/{id}/reject` | Reject a requested payout | Yes (Admin) |
| `POST` | `/api/v1/admin/payouts/{id}/pay` | Mark an approved payout as paid | Yes (Admin) |
| `GET` | `/api/v1/admin/payouts/requests` | Admin: list payout requests | Yes |
| `POST` | `/api/v1/admin/payouts/requests/bulk/approve` | Admin: bulk approve payout requests | Yes |
| `POST` | `/api/v1/admin/payouts/requests/bulk/pay` | Admin: bulk mark payout requests paid | Yes |
| `POST` | `/api/v1/admin/payouts/requests/bulk/reject` | Admin: bulk reject payout requests | Yes |
| `GET` | `/api/v1/admin/payouts/requests/{id}` | Admin: payout request detail | Yes |
| `POST` | `/api/v1/admin/payouts/requests/{id}/approve` | Admin: approve a payout request | Yes |
| `POST` | `/api/v1/admin/payouts/requests/{id}/pay` | Admin: mark a payout request paid | Yes |
| `POST` | `/api/v1/admin/payouts/requests/{id}/reject` | Admin: reject a payout request | Yes |

## Lifecycle

```
REQUESTED --approve--> APPROVED --pay--> PAID
    |                       |
    +-------reject----------+--> REJECTED
```

- A seller can only have one open request at a time; the balance is reserved when the request is created.
- Rejecting a request releases the reserved balance.
- `pay` is the only transition that records the external transfer; it requires `APPROVED` status.
- Suspended sellers and shops without `APPROVED` status cannot request payouts.

## Request a payout

```bash
curl -X 'POST' \
  'http://localhost:8080/api/v1/seller/payouts/request' \
  -H 'Authorization: Bearer <seller_token>' \
  -H 'Idempotency-Key: payout-2026-01-15-001' \
  -H 'Content-Type: application/json' \
  -d '{"amount": "250.00"}'
```

## Admin marks a payout paid

```bash
curl -X 'POST' \
  'http://localhost:8080/api/v1/admin/payouts/8f2c1a44-0001/approve' \
  -H 'Authorization: Bearer <admin_token>'

curl -X 'POST' \
  'http://localhost:8080/api/v1/admin/payouts/8f2c1a44-0001/pay' \
  -H 'Authorization: Bearer <admin_token>'
```
## Endpoint Details

### 1. Seller: list payouts

**`GET /api/v1/seller/payouts`**

Seller: list payouts

#### Request

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/seller/payouts?page=1&perPage=20&status=PENDING&from=2026-05-01&to=2026-05-31' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <seller_token>'
```

#### Response

```json
{
  "data": [
    {
      "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "sellerId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "orderId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "subTotal": "250.00",
      "commissionAmount": "250.00",
      "payoutAmount": "250.00",
      "currency": "USD",
      "status": "PENDING",
      "payoutRequestId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "rejectionReason": "Policy violation",
      "orderNumber": "ORD-20240506-0001-A1B2C3D4",
      "sellerEmail": "jane@example.com",
      "allowedActions": [
        "APPROVE"
      ],
      "requestedAt": "2024-05-06T12:00:00",
      "approvedAt": "2024-05-06T12:00:00",
      "rejectedAt": "2024-05-06T12:00:00",
      "paidAt": "2024-05-06T12:00:00",
      "createdAt": "2024-05-06T12:00:00"
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

### 2. Seller: available balance

**`GET /api/v1/seller/payouts/balance`**

Seller: available balance

#### Request

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/seller/payouts/balance' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <seller_token>'
```

#### Response

```json
{
  "available": "string",
  "currency": "USD",
  "pendingRequestAmount": "250.00",
  "balances": {}
}
```

---

### 3. Seller: request payout

**`POST /api/v1/seller/payouts/request`**

Seller: request payout

#### Request

```bash
curl -X 'POST' \
  'http://localhost:8080/api/v1/seller/payouts/request' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <seller_token>' \
  -H 'Content-Type: application/json' \
  -d '{
  "amount": "250.00",
  "currency": "USD"
}'
```

#### Response

```json
[
  {
    "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
    "sellerId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
    "orderId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
    "subTotal": "250.00",
    "commissionAmount": "250.00",
    "payoutAmount": "250.00",
    "currency": "USD",
    "status": "PENDING",
    "payoutRequestId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
    "rejectionReason": "Policy violation",
    "orderNumber": "ORD-20240506-0001-A1B2C3D4",
    "sellerEmail": "jane@example.com",
    "allowedActions": [
      "APPROVE"
    ],
    "requestedAt": "2024-05-06T12:00:00",
    "approvedAt": "2024-05-06T12:00:00",
    "rejectedAt": "2024-05-06T12:00:00",
    "paidAt": "2024-05-06T12:00:00",
    "createdAt": "2024-05-06T12:00:00"
  }
]
```

---

### 4. Seller: payout statement

**`GET /api/v1/seller/payouts/statement`**

Seller: payout statement

#### Request

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/seller/payouts/statement?from=2026-05-01&to=2026-05-31' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <seller_token>'
```

#### Response

```json
{
  "from": "string",
  "to": "string",
  "currency": "USD",
  "grossSales": "string",
  "commission": "string",
  "netPayout": "string",
  "paid": 1,
  "pending": "string",
  "available": "string",
  "taxCollected": "string",
  "rowCount": 1,
  "rows": [
    {
      "payoutId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "orderId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "subTotal": "250.00",
      "commissionAmount": "250.00",
      "payoutAmount": "250.00",
      "currency": "USD",
      "status": "PENDING",
      "createdAt": "2024-05-06T12:00:00"
    }
  ]
}
```

---

### 5. Seller: export statement CSV

**`GET /api/v1/seller/payouts/statement/export`**

Seller: export statement CSV

#### Request

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/seller/payouts/statement/export?from=2026-05-01&to=2026-05-31' \
  -H 'accept: text/csv' \
  -H 'Authorization: Bearer <seller_token>'
```

#### Response

```csv
id,status,amount,currency,createdAt
example-id,PAID,250.00,USD,2024-05-06T12:00:00
```

---

### 6. Seller: list payout requests

**`GET /api/v1/seller/payouts/requests`**

Seller: list payout requests

#### Request

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/seller/payouts/requests?page=1&perPage=20&status=PENDING' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <seller_token>'
```

#### Response

```json
{
  "data": [
    {
      "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "sellerId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "sellerEmail": "jane@example.com",
      "amount": "250.00",
      "currency": "USD",
      "status": "PENDING",
      "rejectionReason": "Policy violation",
      "rowCount": 1,
      "allowedActions": [
        "APPROVE"
      ],
      "createdAt": "2024-05-06T12:00:00"
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

### 7. Seller: payout request detail

**`GET /api/v1/seller/payouts/requests/{id}`**

Seller: payout request detail

#### Request

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/seller/payouts/requests/6c5078d3-f8e3-4c88-9afe-48b5423c664f' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <seller_token>'
```

#### Response

```json
{
  "request": {
    "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
    "sellerId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
    "sellerEmail": "jane@example.com",
    "amount": "250.00",
    "currency": "USD",
    "status": "PENDING",
    "rejectionReason": "Policy violation",
    "rowCount": 1,
    "allowedActions": [
      "APPROVE"
    ],
    "createdAt": "2024-05-06T12:00:00"
  },
  "rows": [
    {
      "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "sellerId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "orderId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "subTotal": "250.00",
      "commissionAmount": "250.00",
      "payoutAmount": "250.00",
      "currency": "USD",
      "status": "PENDING",
      "payoutRequestId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "rejectionReason": "Policy violation",
      "orderNumber": "ORD-20240506-0001-A1B2C3D4",
      "sellerEmail": "jane@example.com",
      "allowedActions": [
        "APPROVE"
      ],
      "requestedAt": "2024-05-06T12:00:00",
      "approvedAt": "2024-05-06T12:00:00",
      "rejectedAt": "2024-05-06T12:00:00",
      "paidAt": "2024-05-06T12:00:00",
      "createdAt": "2024-05-06T12:00:00"
    }
  ]
}
```

---
### 8. Admin: list payouts

**`GET /api/v1/admin/payouts`**

Admin: list payouts

#### Request

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/admin/payouts?page=1&perPage=20&status=PENDING&seller=6c5078d3-f8e3-4c88-9afe-48b5423c664f&from=2026-05-01&to=2026-05-31' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <admin_token>'
```

#### Response

```json
{
  "data": [
    {
      "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "sellerId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "orderId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "subTotal": "250.00",
      "commissionAmount": "250.00",
      "payoutAmount": "250.00",
      "currency": "USD",
      "status": "PENDING",
      "payoutRequestId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "rejectionReason": "Policy violation",
      "orderNumber": "ORD-20240506-0001-A1B2C3D4",
      "sellerEmail": "jane@example.com",
      "allowedActions": [
        "APPROVE"
      ],
      "requestedAt": "2024-05-06T12:00:00",
      "approvedAt": "2024-05-06T12:00:00",
      "rejectedAt": "2024-05-06T12:00:00",
      "paidAt": "2024-05-06T12:00:00",
      "createdAt": "2024-05-06T12:00:00"
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

### 9. Admin: export payouts CSV

**`GET /api/v1/admin/payouts/export`**

Admin: export payouts CSV

#### Request

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/admin/payouts/export?status=PENDING&seller=6c5078d3-f8e3-4c88-9afe-48b5423c664f&from=2026-05-01&to=2026-05-31' \
  -H 'accept: text/csv' \
  -H 'Authorization: Bearer <admin_token>'
```

#### Response

```csv
id,status,amount,currency,createdAt
example-id,PAID,250.00,USD,2024-05-06T12:00:00
```

---

### 10. Admin: approve payout row

**`POST /api/v1/admin/payouts/{id}/approve`**

Admin: approve payout row

#### Request

```bash
curl -X 'POST' \
  'http://localhost:8080/api/v1/admin/payouts/6c5078d3-f8e3-4c88-9afe-48b5423c664f/approve' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <admin_token>'
```

#### Response

```json
{
  "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "sellerId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "orderId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "subTotal": "250.00",
  "commissionAmount": "250.00",
  "payoutAmount": "250.00",
  "currency": "USD",
  "status": "PENDING",
  "payoutRequestId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "rejectionReason": "Policy violation",
  "orderNumber": "ORD-20240506-0001-A1B2C3D4",
  "sellerEmail": "jane@example.com",
  "allowedActions": [
    "APPROVE"
  ],
  "requestedAt": "2024-05-06T12:00:00",
  "approvedAt": "2024-05-06T12:00:00",
  "rejectedAt": "2024-05-06T12:00:00",
  "paidAt": "2024-05-06T12:00:00",
  "createdAt": "2024-05-06T12:00:00"
}
```

---

### 11. Admin: reject payout row

**`POST /api/v1/admin/payouts/{id}/reject`**

Admin: reject payout row

#### Request

```bash
curl -X 'POST' \
  'http://localhost:8080/api/v1/admin/payouts/6c5078d3-f8e3-4c88-9afe-48b5423c664f/reject' \
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
  "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "sellerId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "orderId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "subTotal": "250.00",
  "commissionAmount": "250.00",
  "payoutAmount": "250.00",
  "currency": "USD",
  "status": "PENDING",
  "payoutRequestId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "rejectionReason": "Policy violation",
  "orderNumber": "ORD-20240506-0001-A1B2C3D4",
  "sellerEmail": "jane@example.com",
  "allowedActions": [
    "APPROVE"
  ],
  "requestedAt": "2024-05-06T12:00:00",
  "approvedAt": "2024-05-06T12:00:00",
  "rejectedAt": "2024-05-06T12:00:00",
  "paidAt": "2024-05-06T12:00:00",
  "createdAt": "2024-05-06T12:00:00"
}
```

---

### 12. Admin: mark payout row paid

**`POST /api/v1/admin/payouts/{id}/pay`**

Admin: mark payout row paid

#### Request

```bash
curl -X 'POST' \
  'http://localhost:8080/api/v1/admin/payouts/6c5078d3-f8e3-4c88-9afe-48b5423c664f/pay' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <admin_token>'
```

#### Response

```json
{
  "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "sellerId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "orderId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "subTotal": "250.00",
  "commissionAmount": "250.00",
  "payoutAmount": "250.00",
  "currency": "USD",
  "status": "PENDING",
  "payoutRequestId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "rejectionReason": "Policy violation",
  "orderNumber": "ORD-20240506-0001-A1B2C3D4",
  "sellerEmail": "jane@example.com",
  "allowedActions": [
    "APPROVE"
  ],
  "requestedAt": "2024-05-06T12:00:00",
  "approvedAt": "2024-05-06T12:00:00",
  "rejectedAt": "2024-05-06T12:00:00",
  "paidAt": "2024-05-06T12:00:00",
  "createdAt": "2024-05-06T12:00:00"
}
```

---

### 13. Admin: list payout requests

**`GET /api/v1/admin/payouts/requests`**

Admin: list payout requests

#### Request

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/admin/payouts/requests?page=1&perPage=20&status=PENDING&seller=6c5078d3-f8e3-4c88-9afe-48b5423c664f' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <admin_token>'
```

#### Response

```json
{
  "data": [
    {
      "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "sellerId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "sellerEmail": "jane@example.com",
      "amount": "250.00",
      "currency": "USD",
      "status": "PENDING",
      "rejectionReason": "Policy violation",
      "rowCount": 1,
      "allowedActions": [
        "APPROVE"
      ],
      "createdAt": "2024-05-06T12:00:00"
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

### 14. Admin: bulk approve payout requests

**`POST /api/v1/admin/payouts/requests/bulk/approve`**

Admin: bulk approve payout requests

#### Request

```bash
curl -X 'POST' \
  'http://localhost:8080/api/v1/admin/payouts/requests/bulk/approve' \
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

### 15. Admin: bulk mark payout requests paid

**`POST /api/v1/admin/payouts/requests/bulk/pay`**

Admin: bulk mark payout requests paid

#### Request

```bash
curl -X 'POST' \
  'http://localhost:8080/api/v1/admin/payouts/requests/bulk/pay' \
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

### 16. Admin: bulk reject payout requests

**`POST /api/v1/admin/payouts/requests/bulk/reject`**

Admin: bulk reject payout requests

#### Request

```bash
curl -X 'POST' \
  'http://localhost:8080/api/v1/admin/payouts/requests/bulk/reject' \
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

### 17. Admin: payout request detail

**`GET /api/v1/admin/payouts/requests/{id}`**

Admin: payout request detail

#### Request

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/admin/payouts/requests/6c5078d3-f8e3-4c88-9afe-48b5423c664f' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <admin_token>'
```

#### Response

```json
{
  "request": {
    "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
    "sellerId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
    "sellerEmail": "jane@example.com",
    "amount": "250.00",
    "currency": "USD",
    "status": "PENDING",
    "rejectionReason": "Policy violation",
    "rowCount": 1,
    "allowedActions": [
      "APPROVE"
    ],
    "createdAt": "2024-05-06T12:00:00"
  },
  "rows": [
    {
      "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "sellerId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "orderId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "subTotal": "250.00",
      "commissionAmount": "250.00",
      "payoutAmount": "250.00",
      "currency": "USD",
      "status": "PENDING",
      "payoutRequestId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "rejectionReason": "Policy violation",
      "orderNumber": "ORD-20240506-0001-A1B2C3D4",
      "sellerEmail": "jane@example.com",
      "allowedActions": [
        "APPROVE"
      ],
      "requestedAt": "2024-05-06T12:00:00",
      "approvedAt": "2024-05-06T12:00:00",
      "rejectedAt": "2024-05-06T12:00:00",
      "paidAt": "2024-05-06T12:00:00",
      "createdAt": "2024-05-06T12:00:00"
    }
  ]
}
```

---

### 18. Admin: approve a payout request

**`POST /api/v1/admin/payouts/requests/{id}/approve`**

Admin: approve a payout request

#### Request

```bash
curl -X 'POST' \
  'http://localhost:8080/api/v1/admin/payouts/requests/6c5078d3-f8e3-4c88-9afe-48b5423c664f/approve' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <admin_token>'
```

#### Response

```json
{
  "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "sellerId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "sellerEmail": "jane@example.com",
  "amount": "250.00",
  "currency": "USD",
  "status": "PENDING",
  "rejectionReason": "Policy violation",
  "rowCount": 1,
  "allowedActions": [
    "APPROVE"
  ],
  "createdAt": "2024-05-06T12:00:00"
}
```

---

### 19. Admin: mark a payout request paid

**`POST /api/v1/admin/payouts/requests/{id}/pay`**

Admin: mark a payout request paid

#### Request

```bash
curl -X 'POST' \
  'http://localhost:8080/api/v1/admin/payouts/requests/6c5078d3-f8e3-4c88-9afe-48b5423c664f/pay' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <admin_token>'
```

#### Response

```json
{
  "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "sellerId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "sellerEmail": "jane@example.com",
  "amount": "250.00",
  "currency": "USD",
  "status": "PENDING",
  "rejectionReason": "Policy violation",
  "rowCount": 1,
  "allowedActions": [
    "APPROVE"
  ],
  "createdAt": "2024-05-06T12:00:00"
}
```

---

### 20. Admin: reject a payout request

**`POST /api/v1/admin/payouts/requests/{id}/reject`**

Admin: reject a payout request

#### Request

```bash
curl -X 'POST' \
  'http://localhost:8080/api/v1/admin/payouts/requests/6c5078d3-f8e3-4c88-9afe-48b5423c664f/reject' \
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
  "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "sellerId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "sellerEmail": "jane@example.com",
  "amount": "250.00",
  "currency": "USD",
  "status": "PENDING",
  "rejectionReason": "Policy violation",
  "rowCount": 1,
  "allowedActions": [
    "APPROVE"
  ],
  "createdAt": "2024-05-06T12:00:00"
}
```

---
