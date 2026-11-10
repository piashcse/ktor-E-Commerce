# Seller KYC

Sellers must submit KYC before requesting payouts. Already-approved sellers are grandfathered as VERIFIED by V26.

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/v1/seller/kyc` | Current KYC state |
| `POST` | `/api/v1/seller/kyc` | Submit business/bank details and optional document URL |
| `GET` | `/api/v1/admin/sellers/kyc?status=PENDING` | Admin KYC queue |
| `PUT` | `/api/v1/admin/sellers/{sellerId}/kyc/verify` | Verify KYC |
| `PUT` | `/api/v1/admin/sellers/{sellerId}/kyc/reject` | Reject with a reason |

States: `NOT_SUBMITTED → PENDING → VERIFIED` or `PENDING → REJECTED`.
