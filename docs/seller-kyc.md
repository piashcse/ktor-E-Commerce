# Seller KYC

Sellers must submit KYC before requesting payouts. Already-approved sellers are grandfathered as
`VERIFIED` by migration `V26`.

## Endpoints

| Method | Endpoint | Description | Authentication Required |
|---|---|---|---|
| `GET` | `/api/v1/seller/kyc` | Current KYC state | Yes (Seller) |
| `POST` | `/api/v1/seller/kyc` | Submit business/bank details and optional document URL | Yes (Seller) |
| `GET` | `/api/v1/admin/sellers?status=PENDING` | Admin KYC queue (optional status filter) | Yes (Admin) |
| `PUT` | `/api/v1/admin/sellers/{sellerId}/verify` | Verify KYC | Yes (Admin) |
| `PUT` | `/api/v1/admin/sellers/{sellerId}/reject` | Reject KYC with a reason | Yes (Admin) |

States: `NOT_SUBMITTED → PENDING → VERIFIED` or `PENDING → REJECTED`.

## Submit KYC

**`POST /api/v1/seller/kyc`**

```json
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

## Verify / reject

**`PUT /api/v1/admin/sellers/{sellerId}/verify`**

Marks the seller `VERIFIED` and records `verifiedAt`.

**`PUT /api/v1/admin/sellers/{sellerId}/reject`**

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
