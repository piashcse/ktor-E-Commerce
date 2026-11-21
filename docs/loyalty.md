# Loyalty, Referrals & Gift Cards

Wallet balances, loyalty points, referrals and gift cards.

All endpoints require Bearer authentication.

## Endpoints

| Method | Endpoint | Description |
|---|---|---|
| <span class="method-badge get">GET</span> | `/api/v1/wallet` | Points, store credit, lifetime points and referral code |
| <span class="method-badge get">GET</span> | `/api/v1/wallet/ledger` | Paginated wallet ledger |
| <span class="method-badge get">GET</span> | `/api/v1/wallet/gift-cards/{code}` | Gift-card balance |
| <span class="method-badge get">GET</span> | `/api/v1/referrals` | Referrals where you are referrer or referee |
| <span class="method-badge post">POST</span> | `/api/v1/referrals/claim` | Claim a referral code |
| <span class="method-badge get">GET</span> | `/api/v1/admin/gift-cards` | List gift cards |
| <span class="method-badge post">POST</span> | `/api/v1/admin/gift-cards` | Issue a gift card |
| <span class="method-badge delete">DELETE</span> | `/api/v1/admin/gift-cards/{id}` | Void a gift card |
| <span class="method-badge post">POST</span> | `/api/v1/admin/wallets/{userId}/credit` | Credit store balance |

---

## Points

- Earn **1 point per 1 USD** of a paid order (`LOYALTY_POINTS_PER_USD`).
- Redeem **100 points = 1 USD** (`LOYALTY_POINTS_PER_USD_REDEEM`).
- Earnings are idempotent per order; replaying a payment confirmation does not double-award.
- Points are awarded when the order becomes fully paid.
- Points are earned on the **USD-normalized** order total.
- Cancellation/full refund reverses earned points, restores redeemed points/store credit and
  refunds gift-card redemptions back to the card.

Checkout body:

```json
{
  "shippingAddressId": "address-uuid",
  "shippingMethodId": "method-uuid",
  "paymentMethod": "CREDIT_CARD",
  "usePoints": 500,
  "walletCredit": 10.00,
  "giftCardCode": "GIFT1234ABCD"
}
```

`walletCredit` is in **USD** and is converted with the order FX rate. Redemptions are applied after
coupons/promotions and capped at the order total **before** points/credit are debited, so unused
balances are never burned. Points/credit/gift-card debits are recorded in
`wallet_ledger`/`gift_card_redemption` and the order stores `walletDiscount` and
`giftCardDiscount`.

Checkout summary returns `walletDiscount`, `giftCardDiscount`, `pointsBalance` and
`storeCreditBalance` so clients can display the applied amounts before placing the order.

## Referrals

Each wallet has a unique `referralCode`.

```bash
curl -X POST 'http://localhost:8080/api/v1/referrals/claim' \
  -H 'Authorization: Bearer <token>' \
  -H 'Content-Type: application/json' \
  -d '{ "code": "REF12345" }'
```

- Self-referral and duplicate claims are rejected.
- The referral stays `PENDING` until the referee's first order is paid.
- On completion both sides receive `REFERRAL_REWARD_POINTS` (default 100) as a
  `REFERRAL_BONUS` ledger entry.
- A user can be a referee only once.

## Gift cards

Admin issue:

```json
{
  "amount": 50.00,
  "currency": "USD",
  "code": "GIFT1234ABCD",
  "expiresAt": "2027-01-01T00:00:00"
}
```

`code` is optional; a unique 16-character code is generated when omitted. Codes are
case-insensitive on redemption. Partial redemptions reduce the balance; a zero balance marks the
card `REDEEMED`. Voided, expired and non-USD cards cannot be redeemed. Unused balance stays on the
card.

## Store credit

Admins can credit a customer for goodwill/refunds:

```json
{ "amount": 25.00, "note": "Goodwill credit" }
```

Store credit is held in USD and recorded as an `ADMIN_ADJUSTMENT` ledger entry.

## Wallet response

```json
{
  "creditBalance": "25.00",
  "pointsBalance": 150,
  "lifetimePoints": 200,
  "referralCode": "REF12345"
}
```
## Endpoint Details

### 1. Get my loyalty wallet

<div class="endpoint-header">
  <span class="method-badge get">GET</span>
  <span class="endpoint-path">/api/v1/wallet</span>
</div>

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/wallet' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <access_token>'
```

#### Response

```json
{
  "creditBalance": "250.00",
  "pointsBalance": 1,
  "lifetimePoints": 1,
  "referralCode": "SAVE10"
}
```

---

### 2. List my wallet ledger

<div class="endpoint-header">
  <span class="method-badge get">GET</span>
  <span class="endpoint-path">/api/v1/wallet/ledger</span>
</div>

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/wallet/ledger?page=1&perPage=20' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <access_token>'
```

#### Response

```json
{
  "data": [
    {
      "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "type": "POINTS_EARN",
      "pointsDelta": 1,
      "creditDelta": "string",
      "referenceType": "CUSTOMER",
      "referenceId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "note": "string",
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

### 3. Check a gift-card balance

<div class="endpoint-header">
  <span class="method-badge get">GET</span>
  <span class="endpoint-path">/api/v1/wallet/gift-cards/{code}</span>
</div>

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/wallet/gift-cards/SAVE10' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <access_token>'
```

#### Response

```json
{
  "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "code": "SAVE10",
  "initialBalance": "250.00",
  "balance": "250.00",
  "currency": "USD",
  "status": "ACTIVE",
  "expiresAt": "2024-05-06T12:00:00",
  "createdAt": "2024-05-06T12:00:00"
}
```

---

### 4. List my referrals

<div class="endpoint-header">
  <span class="method-badge get">GET</span>
  <span class="endpoint-path">/api/v1/referrals</span>
</div>

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/referrals' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <access_token>'
```

#### Response

```json
[
  {
    "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
    "referrerUserId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
    "refereeUserId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
    "code": "SAVE10",
    "status": "PENDING",
    "rewardPoints": 1,
    "rewardCredit": "string",
    "completedAt": "2024-05-06T12:00:00",
    "createdAt": "2024-05-06T12:00:00"
  }
]
```

---

### 5. Claim a referral code

<div class="endpoint-header">
  <span class="method-badge post">POST</span>
  <span class="endpoint-path">/api/v1/referrals/claim</span>
</div>

```bash
curl -X 'POST' \
  'http://localhost:8080/api/v1/referrals/claim' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <access_token>' \
  -H 'Content-Type: application/json' \
  -d '{
  "code": "SAVE10"
}'
```

#### Response

```json
{
  "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "referrerUserId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "refereeUserId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "code": "SAVE10",
  "status": "PENDING",
  "rewardPoints": 1,
  "rewardCredit": "string",
  "completedAt": "2024-05-06T12:00:00",
  "createdAt": "2024-05-06T12:00:00"
}
```

---

### 6. List gift cards

<div class="endpoint-header">
  <span class="method-badge get">GET</span>
  <span class="endpoint-path">/api/v1/admin/gift-cards</span>
</div>

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/admin/gift-cards?page=1&perPage=20' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <admin_token>'
```

#### Response

```json
{
  "data": [
    {
      "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "code": "SAVE10",
      "initialBalance": "250.00",
      "balance": "250.00",
      "currency": "USD",
      "status": "ACTIVE",
      "expiresAt": "2024-05-06T12:00:00",
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

### 7. Issue a gift card

<div class="endpoint-header">
  <span class="method-badge post">POST</span>
  <span class="endpoint-path">/api/v1/admin/gift-cards</span>
</div>

```bash
curl -X 'POST' \
  'http://localhost:8080/api/v1/admin/gift-cards' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <admin_token>' \
  -H 'Content-Type: application/json' \
  -d '{
  "amount": "250.00",
  "currency": "USD",
  "code": "SAVE10",
  "expiresAt": "2024-05-06T12:00:00"
}'
```

#### Response

```json
{
  "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "code": "SAVE10",
  "initialBalance": "250.00",
  "balance": "250.00",
  "currency": "USD",
  "status": "ACTIVE",
  "expiresAt": "2024-05-06T12:00:00",
  "createdAt": "2024-05-06T12:00:00"
}
```

---

### 8. Void a gift card

<div class="endpoint-header">
  <span class="method-badge delete">DELETE</span>
  <span class="endpoint-path">/api/v1/admin/gift-cards/{id}</span>
</div>

```bash
curl -X 'DELETE' \
  'http://localhost:8080/api/v1/admin/gift-cards/6c5078d3-f8e3-4c88-9afe-48b5423c664f' \
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

### 9. Credit a customer's store balance

<div class="endpoint-header">
  <span class="method-badge post">POST</span>
  <span class="endpoint-path">/api/v1/admin/wallets/{userId}/credit</span>
</div>

```bash
curl -X 'POST' \
  'http://localhost:8080/api/v1/admin/wallets/6c5078d3-f8e3-4c88-9afe-48b5423c664f/credit' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <admin_token>' \
  -H 'Content-Type: application/json' \
  -d '{
  "amount": "250.00",
  "note": "string"
}'
```

#### Response

```json
{
  "creditBalance": "250.00",
  "pointsBalance": 1,
  "lifetimePoints": 1,
  "referralCode": "SAVE10"
}
```

---
