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

## Admin endpoints

| Method | Endpoint | Description | Authentication Required |
|---|---|---|---|
| `GET` | `/api/v1/admin/payouts` | List all payout requests (optional `status` filter) | Yes (Admin) |
| `GET` | `/api/v1/admin/payouts/export` | Export payouts as CSV | Yes (Admin) |
| `POST` | `/api/v1/admin/payouts/{id}/approve` | Approve a requested payout | Yes (Admin) |
| `POST` | `/api/v1/admin/payouts/{id}/reject` | Reject a requested payout | Yes (Admin) |
| `POST` | `/api/v1/admin/payouts/{id}/pay` | Mark an approved payout as paid | Yes (Admin) |

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

## Example: request a payout

```bash
curl -X 'POST' \
  'http://localhost:8080/api/v1/seller/payouts/request' \
  -H 'Authorization: Bearer <seller_token>' \
  -H 'Idempotency-Key: payout-2026-01-15-001' \
  -H 'Content-Type: application/json' \
  -d '{"amount": "250.00"}'
```

## Example: admin marks a payout paid

```bash
curl -X 'POST' \
  'http://localhost:8080/api/v1/admin/payouts/8f2c1a44-0001/approve' \
  -H 'Authorization: Bearer <admin_token>'

curl -X 'POST' \
  'http://localhost:8080/api/v1/admin/payouts/8f2c1a44-0001/pay' \
  -H 'Authorization: Bearer <admin_token>'
```
