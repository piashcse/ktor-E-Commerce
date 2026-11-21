# Fraud, Risk & Chargebacks

Velocity rules, blocklists, payment risk checks and chargebacks.

All endpoints require an admin Bearer token.

## Endpoints

| Method | Endpoint | Description |
|---|---|---|
| <span class="method-badge get">GET</span> | `/api/v1/admin/fraud/rules` | List velocity rules |
| <span class="method-badge post">POST</span> | `/api/v1/admin/fraud/rules` | Create a rule |
| <span class="method-badge put">PUT</span> | `/api/v1/admin/fraud/rules/{id}` | Update a rule |
| <span class="method-badge delete">DELETE</span> | `/api/v1/admin/fraud/rules/{id}` | Delete a rule |
| <span class="method-badge get">GET</span> | `/api/v1/admin/fraud/blocklist` | List blocklist entries |
| <span class="method-badge post">POST</span> | `/api/v1/admin/fraud/blocklist` | Add a blocklist entry |
| <span class="method-badge delete">DELETE</span> | `/api/v1/admin/fraud/blocklist/{id}` | Delete a blocklist entry |
| <span class="method-badge get">GET</span> | `/api/v1/admin/fraud/high-risk` | List payments flagged for review/block |
| <span class="method-badge post">POST</span> | `/api/v1/admin/payments/{id}/risk` | Record AVS/CVV results |
| <span class="method-badge get">GET</span> | `/api/v1/admin/chargebacks` | List chargebacks |
| <span class="method-badge post">POST</span> | `/api/v1/admin/chargebacks` | Record a chargeback |
| <span class="method-badge get">GET</span> | `/api/v1/admin/chargebacks/{id}` | Retrieve a chargeback |
| <span class="method-badge put">PUT</span> | `/api/v1/admin/chargebacks/{id}` | Update status/outcome |
| <span class="method-badge get">GET</span> | `/api/v1/admin/chargebacks/export` | Export chargebacks CSV |
| <span class="method-badge post">POST</span> | `/api/v1/admin/chargebacks/{id}/evidence` | Attach evidence to a chargeback |

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
## Endpoint Details

### 1. List fraud velocity rules

<div class="endpoint-header">
  <span class="method-badge get">GET</span>
  <span class="endpoint-path">/api/v1/admin/fraud/rules</span>
</div>

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/admin/fraud/rules' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <admin_token>'
```

#### Response

```json
[
  {
    "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
    "name": "Example",
    "metric": "ORDER_COUNT",
    "windowMinutes": 1,
    "threshold": "string",
    "action": "ALLOW",
    "priority": 1,
    "isActive": true
  }
]
```

---

### 2. Create a fraud velocity rule

<div class="endpoint-header">
  <span class="method-badge post">POST</span>
  <span class="endpoint-path">/api/v1/admin/fraud/rules</span>
</div>

```bash
curl -X 'POST' \
  'http://localhost:8080/api/v1/admin/fraud/rules' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <admin_token>' \
  -H 'Content-Type: application/json' \
  -d '{
  "name": "Example",
  "metric": "ORDER_COUNT",
  "windowMinutes": 1,
  "threshold": "string",
  "action": "ALLOW",
  "priority": 1,
  "isActive": true
}'
```

#### Response

```json
{
  "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "name": "Example",
  "metric": "ORDER_COUNT",
  "windowMinutes": 1,
  "threshold": "string",
  "action": "ALLOW",
  "priority": 1,
  "isActive": true
}
```

---

### 3. Update a fraud velocity rule

<div class="endpoint-header">
  <span class="method-badge put">PUT</span>
  <span class="endpoint-path">/api/v1/admin/fraud/rules/{id}</span>
</div>

```bash
curl -X 'PUT' \
  'http://localhost:8080/api/v1/admin/fraud/rules/6c5078d3-f8e3-4c88-9afe-48b5423c664f' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <admin_token>' \
  -H 'Content-Type: application/json' \
  -d '{
  "name": "Example",
  "metric": "ORDER_COUNT",
  "windowMinutes": 1,
  "threshold": "string",
  "action": "ALLOW",
  "priority": 1,
  "isActive": true
}'
```

#### Response

```json
{
  "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "name": "Example",
  "metric": "ORDER_COUNT",
  "windowMinutes": 1,
  "threshold": "string",
  "action": "ALLOW",
  "priority": 1,
  "isActive": true
}
```

---

### 4. Delete a fraud velocity rule

<div class="endpoint-header">
  <span class="method-badge delete">DELETE</span>
  <span class="endpoint-path">/api/v1/admin/fraud/rules/{id}</span>
</div>

```bash
curl -X 'DELETE' \
  'http://localhost:8080/api/v1/admin/fraud/rules/6c5078d3-f8e3-4c88-9afe-48b5423c664f' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <admin_token>'
```

#### Response

```json
{
  "message": "Operation completed"
}
```

---

### 5. List blocklist entries

<div class="endpoint-header">
  <span class="method-badge get">GET</span>
  <span class="endpoint-path">/api/v1/admin/fraud/blocklist</span>
</div>

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/admin/fraud/blocklist' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <admin_token>'
```

#### Response

```json
[
  {
    "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
    "type": "USER",
    "value": "10.00",
    "reason": "Policy violation",
    "isActive": true,
    "expiresAt": "2024-05-06T12:00:00",
    "createdAt": "2024-05-06T12:00:00"
  }
]
```

---

### 6. Add a blocklist entry

<div class="endpoint-header">
  <span class="method-badge post">POST</span>
  <span class="endpoint-path">/api/v1/admin/fraud/blocklist</span>
</div>

```bash
curl -X 'POST' \
  'http://localhost:8080/api/v1/admin/fraud/blocklist' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <admin_token>' \
  -H 'Content-Type: application/json' \
  -d '{
  "type": "USER",
  "value": "10.00",
  "reason": "Policy violation",
  "expiresAt": "2024-05-06T12:00:00",
  "isActive": true
}'
```

#### Response

```json
{
  "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "type": "USER",
  "value": "10.00",
  "reason": "Policy violation",
  "isActive": true,
  "expiresAt": "2024-05-06T12:00:00",
  "createdAt": "2024-05-06T12:00:00"
}
```

---

### 7. Delete a blocklist entry

<div class="endpoint-header">
  <span class="method-badge delete">DELETE</span>
  <span class="endpoint-path">/api/v1/admin/fraud/blocklist/{id}</span>
</div>

```bash
curl -X 'DELETE' \
  'http://localhost:8080/api/v1/admin/fraud/blocklist/6c5078d3-f8e3-4c88-9afe-48b5423c664f' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <admin_token>'
```

#### Response

```json
{
  "message": "Operation completed"
}
```

---

### 8. List payments flagged for review or blocked

<div class="endpoint-header">
  <span class="method-badge get">GET</span>
  <span class="endpoint-path">/api/v1/admin/fraud/high-risk</span>
</div>

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/admin/fraud/high-risk?page=1&perPage=20' \
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
      "amount": "250.00",
      "currency": "USD",
      "status": "PENDING",
      "paymentMethod": "CREDIT_CARD",
      "transactionId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "createdAt": "2024-05-06T12:00:00",
      "provider": "MANUAL",
      "providerReference": "pi_123456789",
      "clientSecret": "pi_123_secret_456",
      "nextActionUrl": "https://example.com/next",
      "scaStatus": "NOT_REQUIRED",
      "providerStatus": "succeeded",
      "riskScore": 1,
      "riskDecision": "ALLOW",
      "avsStatus": "MATCH",
      "cvvStatus": "MATCH",
      "threeDsEnforced": true
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

### 9. Record AVS/CVV results for a payment

<div class="endpoint-header">
  <span class="method-badge post">POST</span>
  <span class="endpoint-path">/api/v1/admin/payments/{id}/risk</span>
</div>

```bash
curl -X 'POST' \
  'http://localhost:8080/api/v1/admin/payments/6c5078d3-f8e3-4c88-9afe-48b5423c664f/risk' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <admin_token>' \
  -H 'Content-Type: application/json' \
  -d '{
  "avsStatus": "MATCH",
  "cvvStatus": "MATCH"
}'
```

#### Response

```json
{
  "message": "Operation completed"
}
```

---

### 10. List chargebacks

<div class="endpoint-header">
  <span class="method-badge get">GET</span>
  <span class="endpoint-path">/api/v1/admin/chargebacks</span>
</div>

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/admin/chargebacks?page=1&perPage=20&status=PENDING' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <admin_token>'
```

#### Response

```json
{
  "data": [
    {
      "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "paymentId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "orderId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "providerDisputeId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "amount": "250.00",
      "currency": "USD",
      "status": "OPEN",
      "reason": "Policy violation",
      "outcome": "Policy violation",
      "evidenceDueAt": "2024-05-06T12:00:00",
      "evidenceUrl": "https://example.com/evidence.pdf",
      "openedAt": "2024-05-06T12:00:00",
      "resolvedAt": "2024-05-06T12:00:00",
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

### 11. Record a chargeback/dispute

<div class="endpoint-header">
  <span class="method-badge post">POST</span>
  <span class="endpoint-path">/api/v1/admin/chargebacks</span>
</div>

```bash
curl -X 'POST' \
  'http://localhost:8080/api/v1/admin/chargebacks' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <admin_token>' \
  -H 'Content-Type: application/json' \
  -d '{
  "paymentId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "providerDisputeId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "amount": "250.00",
  "reason": "Policy violation",
  "evidenceDueAt": "2024-05-06T12:00:00"
}'
```

#### Response

```json
{
  "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "paymentId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "orderId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "providerDisputeId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "amount": "250.00",
  "currency": "USD",
  "status": "OPEN",
  "reason": "Policy violation",
  "outcome": "Policy violation",
  "evidenceDueAt": "2024-05-06T12:00:00",
  "evidenceUrl": "https://example.com/evidence.pdf",
  "openedAt": "2024-05-06T12:00:00",
  "resolvedAt": "2024-05-06T12:00:00",
  "createdAt": "2024-05-06T12:00:00"
}
```

---

### 12. Retrieve a chargeback

<div class="endpoint-header">
  <span class="method-badge get">GET</span>
  <span class="endpoint-path">/api/v1/admin/chargebacks/{id}</span>
</div>

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/admin/chargebacks/6c5078d3-f8e3-4c88-9afe-48b5423c664f' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <admin_token>'
```

#### Response

```json
{
  "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "paymentId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "orderId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "providerDisputeId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "amount": "250.00",
  "currency": "USD",
  "status": "OPEN",
  "reason": "Policy violation",
  "outcome": "Policy violation",
  "evidenceDueAt": "2024-05-06T12:00:00",
  "evidenceUrl": "https://example.com/evidence.pdf",
  "openedAt": "2024-05-06T12:00:00",
  "resolvedAt": "2024-05-06T12:00:00",
  "createdAt": "2024-05-06T12:00:00"
}
```

---

### 13. Update a chargeback status/outcome

<div class="endpoint-header">
  <span class="method-badge put">PUT</span>
  <span class="endpoint-path">/api/v1/admin/chargebacks/{id}</span>
</div>

```bash
curl -X 'PUT' \
  'http://localhost:8080/api/v1/admin/chargebacks/6c5078d3-f8e3-4c88-9afe-48b5423c664f' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <admin_token>' \
  -H 'Content-Type: application/json' \
  -d '{
  "status": "OPEN",
  "outcome": "Policy violation",
  "reason": "Policy violation"
}'
```

#### Response

```json
{
  "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "paymentId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "orderId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "providerDisputeId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "amount": "250.00",
  "currency": "USD",
  "status": "OPEN",
  "reason": "Policy violation",
  "outcome": "Policy violation",
  "evidenceDueAt": "2024-05-06T12:00:00",
  "evidenceUrl": "https://example.com/evidence.pdf",
  "openedAt": "2024-05-06T12:00:00",
  "resolvedAt": "2024-05-06T12:00:00",
  "createdAt": "2024-05-06T12:00:00"
}
```

---

### 14. Export chargebacks CSV

<div class="endpoint-header">
  <span class="method-badge get">GET</span>
  <span class="endpoint-path">/api/v1/admin/chargebacks/export</span>
</div>

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/admin/chargebacks/export?status=PENDING' \
  -H 'accept: text/csv' \
  -H 'Authorization: Bearer <admin_token>'
```

#### Response

```csv
id,status,amount,currency,createdAt
example-id,PAID,250.00,USD,2024-05-06T12:00:00
```

---

### 15. Attach evidence to a chargeback

<div class="endpoint-header">
  <span class="method-badge post">POST</span>
  <span class="endpoint-path">/api/v1/admin/chargebacks/{id}/evidence</span>
</div>

```bash
curl -X 'POST' \
  'http://localhost:8080/api/v1/admin/chargebacks/6c5078d3-f8e3-4c88-9afe-48b5423c664f/evidence' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <admin_token>' \
  -H 'Content-Type: application/json' \
  -d '{
  "evidenceUrl": "https://example.com/evidence.pdf"
}'
```

#### Response

```json
{
  "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "paymentId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "orderId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "providerDisputeId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "amount": "250.00",
  "currency": "USD",
  "status": "OPEN",
  "reason": "Policy violation",
  "outcome": "Policy violation",
  "evidenceDueAt": "2024-05-06T12:00:00",
  "evidenceUrl": "https://example.com/evidence.pdf",
  "openedAt": "2024-05-06T12:00:00",
  "resolvedAt": "2024-05-06T12:00:00",
  "createdAt": "2024-05-06T12:00:00"
}
```

---
