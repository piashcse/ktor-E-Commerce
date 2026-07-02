# Checkout

The Checkout module consolidates the process of finalizing an order, including shipping details, total calculations, and order placement.

## Consolidated Workflow
All endpoints are under `/api/v1/checkout` and require `CUSTOMER` authentication.

### 1. Manage Shipping Address
- `POST /api/v1/checkout/shipping-address`: Add a new address.
- `GET /api/v1/checkout/shipping-address`: List saved addresses.
- `PUT /api/v1/checkout/shipping-address/{id}`: Update address.
- `DELETE /api/v1/checkout/shipping-address/{id}`: Remove address.

### 2. Shipping Methods
- `GET /api/v1/checkout/shipping-method`: Retrieve available shipping options (Standard, Express, etc.).

### 3. Order Summary (Real-time Calculation)
`POST /api/v1/checkout/summary`

Calculate totals, taxes, and apply coupons without creating an order. Use this to show the final price to the customer before they confirm.

**Request:**
```json
{
  "shippingAddressId": "uuid",
  "shippingMethodId": "uuid",
  "couponCode": "DISCOUNT10"
}
```

**Response:**
```json
{
  "subTotal": 100.0,
  "shippingCost": 15.0,
  "taxAmount": 5.0,
  "discountAmount": 10.0,
  "total": 110.0,
  "itemCount": 3
}
```

### 4. Place Order
`POST /api/v1/checkout/place-order`

Finalizes the purchase and creates order records.

**Request:**
```json
{
  "shippingAddressId": "uuid",
  "shippingMethodId": "uuid",
  "paymentMethod": "CREDIT_CARD",
  "notes": "Leave at the door",
  "couponCode": "DISCOUNT10",
  "idempotencyKey": "unique-uuid-per-attempt"
}
```

**Response:**
A list of created orders (split by shop if multi-vendor).
