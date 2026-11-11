# Checkout

The Checkout module consolidates the process of finalizing an order, including shipping details, total calculations, and order placement.

## Consolidated Workflow
All endpoints are under `/api/v1/checkout` and require `CUSTOMER` authentication, except
`POST /api/v1/checkout/guest` which is public and feature-flagged (`guest_checkout`).

### 0. Guest Checkout (feature-flagged)
`POST /api/v1/checkout/guest`

Places an order without an account. The request may include a guest email; if an account with that
email already exists the order is claimed by it. Disabled when the `guest_checkout` feature flag is off.

**Request:**
```json
{
  "email": "guest@example.com",
  "firstName": "Guest",
  "lastName": "Shopper",
  "phoneNumber": "+8801700000000",
  "streetAddress": "12 Main St",
  "city": "Dhaka",
  "country": "BD",
  "postCode": "1207",
  "shippingMethodId": "uuid",
  "paymentMethod": "COD",
  "currency": "USD",
  "items": [{ "productId": "uuid", "quantity": 1 }]
}
```

Returns `201 Created` with the created order(s).

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
  "subTotal": "100.00",
  "shippingCost": "15.00",
  "taxAmount": "5.00",
  "discountAmount": "10.00",
  "total": "110.00",
  "itemCount": 3
}
```

### 4. Place Order
`POST /api/v1/checkout/place-order`

Finalizes the purchase and creates order records. Send `Idempotency-Key` (or the body
`idempotencyKey`) to make retries safe; totals are server-validated against database prices.

**Request:**
```json
{
  "shippingAddressId": "uuid",
  "shippingMethodId": "uuid",
  "paymentMethod": "CREDIT_CARD",
  "notes": "Leave at the door",
  "couponCode": "DISCOUNT10",
  "usePoints": 500,
  "walletCredit": 10.00,
  "giftCardCode": "GIFT1234ABCD",
  "locale": "fr",
  "idempotencyKey": "unique-uuid-per-attempt"
}
```

**Response:**
A list of created orders (split by shop if multi-vendor).


## Guest Checkout

`POST /api/v1/checkout/guest` places an order without an account. It is gated by the `guest_checkout` feature flag (disabled by default; enable via the super-admin feature-flags API).

A guest order creates an unverified guest CUSTOMER for the checkout email. If that email later registers and verifies the OTP, the account (and order history) is claimed. Guest orders can be tracked with `GET /api/v1/orders/track?orderNumber=...&email=...`.
