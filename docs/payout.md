# Seller Payouts & Statements

Payouts require an APPROVED shop and VERIFIED seller KYC.

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/v1/seller/payouts` | Payout ledger rows |
| `GET` | `/api/v1/seller/payouts/balance` | Available and pending amounts |
| `POST` | `/api/v1/seller/payouts/request` | Request a payout |
| `GET` | `/api/v1/seller/payouts/statement?from=&to=` | Sales/commission/payout/tax statement |
| `GET` | `/api/v1/seller/payouts/statement/export?from=&to=` | Statement CSV export |

Admin lifecycle: `GET /api/v1/admin/payouts`, `POST /api/v1/admin/payouts/{id}/approve`, `.../reject`, `.../pay`, and `GET /api/v1/admin/payouts/export`.
