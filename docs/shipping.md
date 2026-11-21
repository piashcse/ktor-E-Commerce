# Shipping & Carrier

Carrier quotes, label purchase, fulfillment and tracking webhooks.

All endpoints except the tracking webhook require Bearer authentication. A customer may read
their own order's shipments; sellers may quote and purchase labels for their shop; admins may do
both for any order.

## Endpoints

| Method | Endpoint | Description | Access |
|---|---|---|---|
| `POST` | `/api/v1/orders/{id}/shipping-quotes` | Get live carrier rates for an order | Authenticated |
| `GET` | `/api/v1/orders/{id}/shipments` | List the order's parcels | Authenticated |
| `POST` | `/api/v1/orders/{id}/shipments` | Purchase a label and allocate order lines | Seller / Admin |
| `GET` | `/api/v1/shipments/{id}` | Retrieve one shipment | Authenticated |
| `POST` | `/api/v1/shipments/webhook/{provider}` | Carrier tracking webhook | Public |

---

### 1. Rate quote

**`POST /api/v1/orders/{id}/shipping-quotes`**
{
  "origin": {
    "name": "Dhaka Warehouse",
    "streetAddress": "12 Main St",
    "city": "Dhaka",
    "zipCode": "1207",
    "country": "BD"
  },
  "destination": {
    "name": "Customer",
    "streetAddress": "9 Side Rd",
    "city": "Chattogram",
    "zipCode": "4000",
    "country": "BD"
  },
  "parcels": [
    { "length": 10, "width": 8, "height": 4, "weight": 2, "distanceUnit": "in", "massUnit": "lb" }
  ]
}
```

Response:

```json
[
  {
    "id": "rate_123",
    "carrier": "USPS",
    "service": "Priority Mail",
    "amount": "7.50",
    "currency": "USD",
    "estimatedDays": 2
  }
]
```

---

### 2. Purchase a label

**`POST /api/v1/orders/{id}/shipments`**
{
  "rateId": "rate_123",
  "items": [
    { "orderItemId": "item-uuid", "quantity": 1 }
  ]
}
```

Response (`201 Created`):

```json
{
  "id": "shipment-uuid",
  "orderId": "order-uuid",
  "carrier": "USPS",
  "service": "Priority Mail",
  "trackingNumber": "9400111",
  "labelUrl": "https://labels.example/shipment.pdf",
  "rateAmount": "7.50",
  "currency": "USD",
  "status": "LABEL_PURCHASED",
  "shippedAt": "2026-05-01T10:00:00Z",
  "deliveredAt": null,
  "createdAt": "2026-05-01T10:00:00Z",
  "items": [{ "orderItemId": "item-uuid", "quantity": 1 }]
}
```

Rules:

- The order must be `PAID` (or already partly `SHIPPED`).
- A line cannot ship more than `ordered - already shipped`.
- When every unit is allocated, the order becomes `SHIPPED` and gets the latest tracking number.
- Over-shipping and non-seller callers are rejected.
- `idempotencyKey` is optional; a retry with the same key returns the original label instead of
  buying a second one.

---

### 3. List / retrieve shipments

**`GET /api/v1/orders/{id}/shipments`**

---

## Admin webhook replay

- `GET /api/v1/admin/webhooks?source=CARRIER&status=FAILED`
- `POST /api/v1/admin/webhooks/carriers/{id}/replay`

Stored carrier payloads can be re-applied after fixing a mapping/outage. Replay updates the inbox
row and writes an audit entry; it does not require the original signature.

### 4. Tracking webhook

**`POST /api/v1/shipments/webhook/{provider}`**
- Deliveries are deduplicated by `(provider, event_id)`; the event ID is the SHA-256 of the payload,
  so retries are safe.
- `TRANSIT`/`OUT_FOR_DELIVERY` → `IN_TRANSIT`
- `DELIVERED` → `DELIVERED`; the order becomes `DELIVERED` once every parcel is delivered.
- `RETURNED` → `RETURNED`; `FAILURE`/`EXPIRED` → `FAILED`.

```bash
curl -X 'POST' \
  'http://localhost:8080/api/v1/shipments/webhook/shippo' \
  -H 'X-Carrier-Signature: <hmac-or-shared-secret>' \
  -H 'Content-Type: application/json' \
  -d '{"event":"track_updated","data":{"tracking_number":"9400111","tracking_status":{"status":"DELIVERED"}}}'
```

---

## Endpoint Details

### 5. Retrieve a shipment

**`GET /api/v1/shipments/{id}`**

Retrieve a shipment

#### Request

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/shipments/6c5078d3-f8e3-4c88-9afe-48b5423c664f' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <access_token>'
```

#### Response

```json
{
  "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "orderId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "carrier": "string",
  "service": "string",
  "trackingNumber": "1Z999AA10123456784",
  "labelUrl": "string",
  "rateAmount": "250.00",
  "currency": "USD",
  "status": "PENDING",
  "shippedAt": "2024-05-06T12:00:00",
  "deliveredAt": "2024-05-06T12:00:00",
  "createdAt": "2024-05-06T12:00:00",
  "items": [
    {
      "orderItemId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "quantity": 1
    }
  ]
}
```

---

## Configuration

| Variable | Description |
|---|---|
| `SHIPPING_PROVIDER` | `manual` (default) or `shippo` |
| `SHIPPO_API_TOKEN` | Shippo token; required when the provider is `shippo` |
| `SHIPPO_BASE_URL` | API base URL (override for tests/proxies) |
| `CARRIER_WEBHOOK_SECRET` | HMAC/shared secret for tracking webhooks |

With `manual`, rate quotes are empty and label purchase is unavailable; sellers can still use
`POST /api/v1/orders/{id}/shipping` with a manually entered tracking number.

## Shipment status values

| Status | Description |
|---|---|
| `PENDING` | Shipment created but no label yet |
| `LABEL_PURCHASED` | Label bought; awaiting carrier pickup |
| `IN_TRANSIT` | Carrier has the parcel |
| `DELIVERED` | Carrier reported delivery |
| `RETURNED` | Parcel returned to sender |
| `CANCELED` | Shipment canceled |
| `FAILED` | Label or delivery failed |
