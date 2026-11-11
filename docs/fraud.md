# Fraud, Risk & Chargebacks

Velocity rules, blocklists, AVS/CVV capture, 3-D Secure enforcement and dispute records.

**Base URL:** `http://localhost:8080`

All endpoints require an admin Bearer token.

## Endpoints

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/v1/admin/fraud/rules` | List velocity rules |
| `POST` | `/api/v1/admin/fraud/rules` | Create a rule |
| `PUT` | `/api/v1/admin/fraud/rules/{id}` | Update a rule |
| `DELETE` | `/api/v1/admin/fraud/rules/{id}` | Delete a rule |
| `GET` | `/api/v1/admin/fraud/blocklist` | List blocklist entries |
| `POST` | `/api/v1/admin/fraud/blocklist` | Add a blocklist entry |
| `DELETE` | `/api/v1/admin/fraud/blocklist/{id}` | Delete a blocklist entry |
| `GET` | `/api/v1/admin/fraud/high-risk` | List payments flagged for review/block |
| `POST` | `/api/v1/admin/payments/{id}/risk` | Record AVS/CVV results |
| `GET` | `/api/v1/admin/chargebacks` | List chargebacks |
| `POST` | `/api/v1/admin/chargebacks` | Record a chargeback |
| `GET` | `/api/v1/admin/chargebacks/{id}` | Retrieve a chargeback |
| `PUT` | `/api/v1/admin/chargebacks/{id}` | Update status/outcome |

---

## Velocity rules

Metrics:

- `ORDER_COUNT` — orders placed by the customer in the window
- `ORDER_AMOUNT` — sum of order totals in the window
- `PAYMENT_ATTEMPTS` — payment rows created in the window

Actions:

- `ALLOW` — record only
- `REVIEW` — allow the payment but flag it for manual review
- `REQUIRE_3DS` — ask the card provider to enforce 3-D Secure
- `BLOCK` — reject the payment before the provider is called (`403 FRAUD_BLOCKED`)

```json
{
  "name": "More than 5 orders/hour",
  "metric": "ORDER_COUNT",
  "windowMinutes": 60,
  "threshold": 5,
  "action": "REVIEW",
  "priority": 10,
  "isActive": true
}
```

The highest-priority matching rules are evaluated; `BLOCK` wins over `REVIEW`, which wins over
`REQUIRE_3DS`.

## Blocklists

Types: `USER`, `EMAIL`, `IP`. Entries may expire and can be deactivated.

```json
{
  "type": "EMAIL",
  "value": "fraud@example.com",
  "reason": "Confirmed chargeback fraud",
  "expiresAt": "2027-01-01T00:00:00"
}
```

A matching live entry blocks payment creation before any provider call.

## AVS/CVV

Provider results are stored on the payment and in the immutable `payment_risk` history:

```json
{ "avsStatus": "MATCH", "cvvStatus": "MISMATCH" }
```

Values: AVS `MATCH`, `PARTIAL`, `MISMATCH`, `UNKNOWN`; CVV `MATCH`, `MISMATCH`, `UNKNOWN`.

`GET /api/v1/admin/fraud/high-risk` returns payments whose decision is `REVIEW` or `BLOCK`.

## Chargebacks

```json
{
  "paymentId": "payment-uuid",
  "providerDisputeId": "dp_123",
  "amount": 25.00,
  "reason": "Fraudulent card use",
  "evidenceDueAt": "2026-06-01T00:00:00"
}
```

Statuses: `OPEN` → `UNDER_REVIEW` → `WON` / `LOST` / `CLOSED`. Terminal states cannot be changed;
`resolvedAt` is set automatically.

```json
{ "status": "WON", "outcome": "Evidence accepted" }
```

## Payment response fields

`PaymentResponse` now includes:

- `riskScore` (0–100)
- `riskDecision` (`ALLOW`, `REVIEW`, `BLOCK`)
- `avsStatus`, `cvvStatus`
- `threeDsEnforced`

A blocked payment returns `403` with code `FRAUD_BLOCKED`. `REVIEW` payments are created but the
order is **not** finalized until an admin releases the review. The release endpoint sets the decision
to `ALLOW` and finalizes the order if the payment was captured:

```bash
curl -X POST 'http://localhost:8080/api/v1/admin/payments/payment-uuid/risk/approve' \
  -H 'Authorization: Bearer <admin-token>'
```
