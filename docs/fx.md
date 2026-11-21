# FX Rates

Checkout supports a currency per order. Non-USD checkout **fails closed** unless a stored
rate exists; rates are snapshotted onto `order.fx_rate` so refunds and payouts use the same rate.

Rates are expressed as units of the target currency per 1 USD. Supported currencies are
`USD`, `EUR`, `GBP`, `BDT` and `INR`. A rate older than `FX_MAX_AGE_HOURS` (default `168`)
is rejected for new checkout calculations.

## Admin endpoints

| Method | Endpoint | Description | Authentication Required |
|---|---|---|---|
| `GET` | `/api/v1/admin/fx-rates` | List stored rates | Yes (Admin) |
| `PUT` | `/api/v1/admin/fx-rates/{targetCurrency}` | Set/refresh a rate | Yes (Admin) |
| `GET` | `/api/v1/fx-rates` | List FX rates | No |

### 1. Set FX rate

**`PUT /api/v1/admin/fx-rates/{targetCurrency}`**

#### Path Parameters

| Parameter | Type | Required | Description |
|---|---|---|---|
| `targetCurrency` | string | Yes | One of `USD`, `EUR`, `GBP`, `BDT`, `INR` |

#### Request Body

| Field | Type | Required | Description |
|---|---|---|---|
| `rate` | number/string | Yes | Units of the target currency per 1 USD; must be positive |

#### Request

```bash
curl -X 'PUT' \
  'http://localhost:8080/api/v1/admin/fx-rates/EUR' \
  -H 'Authorization: Bearer <admin_token>' \
  -H 'Content-Type: application/json' \
  -d '{"rate": "0.92"}'
```

#### Response

```json
{
  "targetCurrency": "EUR",
  "rate": "0.92",
  "effectiveAt": "2026-01-15T10:30:00"
}
```

#### Errors

| Status | Code | Meaning |
|---|---|---|
| `400` | `VALIDATION_FAILED` | Unsupported currency or non-positive rate |
| `401` | `UNAUTHORIZED` | Missing/invalid token |
| `403` | `FORBIDDEN` | Caller is not an admin |
## Endpoint Details

### 2. List FX rates

**`GET /api/v1/admin/fx-rates`**

List FX rates

#### Request

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/admin/fx-rates' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <admin_token>'
```

#### Response

```json
[
  {
    "targetCurrency": "USD",
    "rate": "250.00",
    "effectiveAt": "2024-05-06T12:00:00"
  }
]
```

---

### 3. List FX rates

**`GET /api/v1/fx-rates`**

List FX rates

#### Request

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/fx-rates' \
  -H 'accept: application/json'
```

#### Response

```json
[
  {
    "targetCurrency": "USD",
    "rate": "250.00",
    "effectiveAt": "2024-05-06T12:00:00"
  }
]
```

---
