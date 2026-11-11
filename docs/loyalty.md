# Loyalty, Referrals & Gift Cards

A unified wallet for loyalty points, store credit, referral rewards and gift cards.

**Base URL:** `http://localhost:8080`

All endpoints require Bearer authentication.

## Endpoints

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/v1/wallet` | Points, store credit, lifetime points and referral code |
| `GET` | `/api/v1/wallet/ledger` | Paginated wallet ledger |
| `GET` | `/api/v1/wallet/gift-cards/{code}` | Gift-card balance |
| `GET` | `/api/v1/referrals` | Referrals where you are referrer or referee |
| `POST` | `/api/v1/referrals/claim` | Claim a referral code |
| `GET` | `/api/v1/admin/gift-cards` | List gift cards |
| `POST` | `/api/v1/admin/gift-cards` | Issue a gift card |
| `DELETE` | `/api/v1/admin/gift-cards/{id}` | Void a gift card |
| `POST` | `/api/v1/admin/wallets/{userId}/credit` | Credit store balance |

---

## Points

- Earn **1 point per 1 USD** of a paid order (`LOYALTY_POINTS_PER_USD`).
- Redeem **100 points = 1 USD** (`LOYALTY_POINTS_PER_USD_REDEEM`).
- Earnings are idempotent per order; replaying a payment confirmation does not double-award.
- Points are awarded when the order becomes fully paid.

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
