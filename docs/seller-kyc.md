# Seller KYC

Seller identity and bank verification required before payouts.

## Endpoints

| Method | Endpoint | Description | Authentication Required |
|---|---|---|---|
| `GET` | `/api/v1/seller/kyc` | Current KYC state | Yes (Seller) |
| `POST` | `/api/v1/seller/kyc` | Submit business/bank details and optional document URL | Yes (Seller) |
| `GET` | `/api/v1/admin/sellers?status=PENDING` | Admin KYC queue (optional status filter) | Yes (Admin) |
| `PUT` | `/api/v1/admin/sellers/{sellerId}/verify` | Verify KYC | Yes (Admin) |
| `PUT` | `/api/v1/admin/sellers/{sellerId}/reject` | Reject KYC with a reason | Yes (Admin) |
| `POST` | `/api/v1/admin/sellers/bulk/reject` | Admin: bulk reject KYC | Yes |
| `POST` | `/api/v1/admin/sellers/bulk/verify` | Admin: bulk verify KYC | Yes |
| `GET` | `/api/v1/admin/sellers/export` | Admin: export KYC submissions CSV | Yes |
| `GET` | `/api/v1/admin/sellers/{sellerId}/history` | Admin: KYC decision history | Yes |
| `GET` | `/api/v1/seller/kyc/history` | Get my KYC submission history | Yes |

States: `NOT_SUBMITTED → PENDING → VERIFIED` or `PENDING → REJECTED`.

### 1. Submit KYC

**`POST /api/v1/seller/kyc`**
{
  "businessName": "ACME Ltd",
  "businessRegistrationNumber": "REG-12345",
  "taxId": "TAX-9988",
  "bankAccountNumber": "0123456789",
  "bankName": "Example Bank",
  "bankRoutingNumber": "110000",
  "documentUrl": "/uploads/kyc/acme.pdf"
}
```

All fields are optional; submitting sets the status to `PENDING`. A `documentUrl` must point to an
already-uploaded document.

### 2. Verify / reject

**`PUT /api/v1/admin/sellers/{sellerId}/verify`**

```json
{
  "reason": "Business registration expired"
}
```

Marks the seller `REJECTED` and stores `rejectionReason`, which is returned by
`GET /api/v1/seller/kyc`.

```bash
curl -X 'PUT' \
  'http://localhost:8080/api/v1/admin/sellers/seller-1/reject' \
  -H 'Authorization: Bearer <admin_token>' \
  -H 'Content-Type: application/json' \
  -d '{"reason":"Business registration expired"}'
```
## Endpoint Details

### 3. Get my KYC status

**`GET /api/v1/seller/kyc`**

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/seller/kyc' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <seller_token>'
```

#### Response

```json
{
  "sellerId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "userId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "email": "jane@example.com",
  "businessName": "string",
  "businessRegistrationNumber": "string",
  "taxId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "bankName": "string",
  "kycStatus": "NOT_SUBMITTED",
  "documentUrl": "string",
  "submittedAt": "2024-05-06T12:00:00",
  "verifiedAt": "2024-05-06T12:00:00",
  "rejectionReason": "Policy violation",
  "shopStatus": "ACTIVE"
}
```

---

### 4. Admin: list KYC submissions

**`GET /api/v1/admin/sellers`**

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/admin/sellers?page=1&perPage=20&status=PENDING' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <admin_token>'
```

#### Response

```json
{
  "data": [
    {
      "sellerId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "userId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "email": "jane@example.com",
      "businessName": "string",
      "businessRegistrationNumber": "string",
      "taxId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "bankName": "string",
      "kycStatus": "NOT_SUBMITTED",
      "documentUrl": "string",
      "submittedAt": "2024-05-06T12:00:00",
      "verifiedAt": "2024-05-06T12:00:00",
      "rejectionReason": "Policy violation",
      "shopStatus": "ACTIVE"
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

### 5. Admin: bulk reject KYC

**`POST /api/v1/admin/sellers/bulk/reject`**

```bash
curl -X 'POST' \
  'http://localhost:8080/api/v1/admin/sellers/bulk/reject' \
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

### 6. Admin: bulk verify KYC

**`POST /api/v1/admin/sellers/bulk/verify`**

```bash
curl -X 'POST' \
  'http://localhost:8080/api/v1/admin/sellers/bulk/verify' \
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

### 7. Admin: export KYC submissions CSV

**`GET /api/v1/admin/sellers/export`**

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/admin/sellers/export?status=PENDING' \
  -H 'accept: text/csv' \
  -H 'Authorization: Bearer <admin_token>'
```

#### Response

```csv
id,status,amount,currency,createdAt
example-id,PAID,250.00,USD,2024-05-06T12:00:00
```

---

### 8. Admin: KYC decision history

**`GET /api/v1/admin/sellers/{sellerId}/history`**

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/admin/sellers/6c5078d3-f8e3-4c88-9afe-48b5423c664f/history' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <admin_token>'
```

#### Response

```json
[
  {
    "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
    "status": "PENDING",
    "reason": "Policy violation",
    "actorId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
    "createdAt": "2024-05-06T12:00:00"
  }
]
```

---

### 9. Get my KYC submission history

**`GET /api/v1/seller/kyc/history`**

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/seller/kyc/history' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <seller_token>'
```

#### Response

```json
[
  {
    "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
    "status": "PENDING",
    "reason": "Policy violation",
    "actorId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
    "createdAt": "2024-05-06T12:00:00"
  }
]
```

---
### 10. Admin: reject KYC

**`PUT /api/v1/admin/sellers/{sellerId}/reject`**

Admin: reject KYC

#### Request

```bash
curl -X 'PUT' \
  'http://localhost:8080/api/v1/admin/sellers/6c5078d3-f8e3-4c88-9afe-48b5423c664f/reject' \
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
  "sellerId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "userId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "email": "jane@example.com",
  "businessName": "string",
  "businessRegistrationNumber": "string",
  "taxId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "bankName": "string",
  "kycStatus": "NOT_SUBMITTED",
  "documentUrl": "string",
  "submittedAt": "2024-05-06T12:00:00",
  "verifiedAt": "2024-05-06T12:00:00",
  "rejectionReason": "Policy violation",
  "shopStatus": "ACTIVE"
}
```

---
