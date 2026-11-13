# BOPIS, POS & Store Locator

Omnichannel pickup, in-store sales and nearby store discovery.

**Base URL:** `http://localhost:8080`

## Store locator

**`GET /api/v1/stores?lat=23.81&lng=90.41&radiusKm=10&limit=20`**

Lists active warehouses with `supportsPickup = true`.

- Without coordinates, stores are returned without distance.
- With coordinates, results include `distanceKm` (Haversine) and are sorted nearest-first.
- `radiusKm` filters stores with a known distance.
- `limit` is clamped to `1..100`.

```json
[
  {
    "id": "warehouse-uuid",
    "shopId": "shop-uuid",
    "name": "Dhaka Store",
    "address": "12 Main St",
    "city": "Dhaka",
    "country": "BD",
    "latitude": "23.810300",
    "longitude": "90.412500",
    "phone": "+8801700000000",
    "openingHours": "09:00-21:00",
    "supportsPickup": true,
    "supportsPos": true,
    "distanceKm": 1.2
  }
]
```

Seller warehouse create/update now accepts:

```json
{
  "name": "Dhaka Store",
  "code": "DHAKA-01",
  "address": "12 Main St",
  "city": "Dhaka",
  "country": "BD",
  "latitude": 23.8103,
  "longitude": 90.4125,
  "phone": "+8801700000000",
  "openingHours": "09:00-21:00",
  "supportsPickup": true,
  "supportsPos": true
}
```

## BOPIS (buy online, pick up in store)

Checkout accepts:

```json
{
  "shippingAddressId": "address-uuid",
  "shippingMethodId": "method-uuid",
  "fulfillmentType": "PICKUP",
  "pickupWarehouseId": "warehouse-uuid",
  "paymentMethod": "CREDIT_CARD"
}
```

- The warehouse must be active, support pickup, and belong to the selling shop.
- Pickup orders must contain items from one shop.
- `shippingAddressId`, `shippingMethodId` and shipping cost are no longer required for pickup.
- Stock is checked and decremented at the selected pickup warehouse.
- Pickup readiness/completion requires a paid, non-canceled order.
- Shipping cost is zero and the order shipping method is `PICKUP`.
- A 6-character `pickupCode` and `pickupStatus = PENDING` are stored on the order.
- Order responses expose `fulfillmentType`, `pickupWarehouseId`, `pickupCode`, `pickupStatus` and `pickedUpAt`.

Seller flow:

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/v1/seller/pickup-orders` | List pickup orders |
| `POST` | `/api/v1/seller/orders/{id}/pickup/ready` | Mark ready for collection |
| `POST` | `/api/v1/seller/orders/{id}/pickup/complete` | Verify code and complete |

Complete body:

```json
{ "pickupCode": "ABC123" }
```

Completion sets `pickupStatus = PICKED_UP`, the order status to `RECEIVED`, and records
`pickedUpAt`.

## POS sales

**`POST /api/v1/seller/pos/sales`**

```json
{
  "items": [
    { "productId": "product-uuid", "quantity": 2 }
  ],
  "warehouseId": "warehouse-uuid",
  "paymentMethod": "COD",
  "customerEmail": "walkin@example.com",
  "posSessionId": "session-uuid"
}
```

- Optionally validated against a POS-enabled warehouse owned by the seller.
- All items must belong to the seller’s shop.
- Items may include `variantId`; variant prices and warehouse stock are used.
- Creates a completed order with `fulfillmentType = POS`, `shippingMethod = POS` and zero shipping.
- Tax is calculated from the POS warehouse country and the normal tax rules.
- `idempotencyKey` can be supplied to make retries safe.
- Stock is decremented at the selected POS warehouse.
- `posSessionId` links the sale to an open cash-drawer session; `COD` is treated as cash and other
  methods as card for drawer reconciliation.
- Decrements stock, increments sales counters, records a completed manual payment, grants digital
  entitlements, awards loyalty points and emits the normal payment-completed event (seller payouts,
  commission and notifications).
- Returns `201 Created` with the order.

**`GET /api/v1/seller/pos/sales`** lists the seller’s POS sales with pagination.


## POS cash-drawer sessions

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/v1/seller/pos/sessions` | List drawer sessions |
| `POST` | `/api/v1/seller/pos/sessions/open` | Open a drawer session |
| `POST` | `/api/v1/seller/pos/sessions/{id}/close` | Count cash and close |
| `GET` | `/api/v1/seller/pos/sales/{id}/receipt` | Printable sale receipt |
| `GET` | `/api/v1/seller/pos/sessions/{id}/report` | Printable session/Z report |

Open:

```json
{
  "openingFloat": 100.00,
  "warehouseId": "warehouse-uuid",
  "notes": "Morning shift"
}
```

Close:

```json
{
  "countedCash": 121.00,
  "notes": "Counted by Alice"
}
```

- One open session is allowed per shop.
- Cash sales (`COD`) and card sales are accumulated separately.
- Closing records `expectedCash = openingFloat + cashSales`, `countedCash` and `variance`.
- Sessions remain available for POS history/export.
- Receipts and session reports are returned as printable HTML (`text/html`) and only for the
  seller's own shop.
