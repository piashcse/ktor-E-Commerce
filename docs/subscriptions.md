# Subscriptions & Digital Goods

Recurring plans, invoices and download entitlements for digital products.

**Base URL:** `http://localhost:8080`

## Plans

A plan is attached to a digital product and bills monthly or yearly.

```json
{
  "name": "Pro Monthly",
  "description": "Monthly access to the Pro pack",
  "productId": "product-uuid",
  "interval": "MONTH",
  "price": 9.99,
  "currency": "USD",
  "trialDays": 0,
  "isActive": true
}
```

Public: `GET /api/v1/subscription-plans?productId=...`

Admin CRUD:

- `GET /api/v1/admin/subscription-plans`
- `POST /api/v1/admin/subscription-plans`
- `PUT /api/v1/admin/subscription-plans/{id}`
- `DELETE /api/v1/admin/subscription-plans/{id}` — deactivates

## Subscribing

**`POST /api/v1/subscriptions`**

```json
{
  "planId": "plan-uuid",
  "paymentReference": "pay_123"
}
```

- With a `paymentReference`, creates an `ACTIVE` subscription with a paid invoice.
- Without a reference, creates a `PENDING` subscription and a `PENDING` invoice; no entitlement is granted until payment is recorded.
- Period starts now and ends after one month/year (plus trial days).
- Grants a digital entitlement when the plan’s product is digital and payment is confirmed.
- Rejects duplicate active subscriptions for the same plan.

Other endpoints:

- `GET /api/v1/subscriptions`
- `GET /api/v1/subscriptions/{id}/invoices`
- `POST /api/v1/subscriptions/{id}/cancel` — marks `cancelAtPeriodEnd`

## Renewal billing

**`POST /api/v1/admin/subscriptions/run-billing`**

Processes every `ACTIVE` subscription whose period has ended:

- `cancelAtPeriodEnd = true` → status becomes `CANCELED`
- otherwise: creates a `PENDING` renewal invoice and sets the subscription `PAST_DUE`

Returns `{ "invoicesCreated": N, "canceled": M }`.

Confirm the renewal payment and reactivate:

**`POST /api/v1/admin/subscriptions/{id}/activate`**

```json
{ "paymentReference": "pay_renewal" }
```

Marks the pending invoice paid, advances the period, sets the subscription `ACTIVE`, and refreshes
the digital entitlement expiry.

## Digital goods

Products can be marked digital:

```json
{
  "isDigital": true,
  "digitalAssetUrl": "https://downloads.example/pack.zip",
  "digitalFileName": "pack.zip"
}
```

Digital-only orders have **zero shipping cost** and the order shipping method is stored as
`DIGITAL`. When payment completes, an entitlement is created for each digital product in the order.

### Library

**`GET /api/v1/library`**

```json
[
  {
    "entitlementId": "ent-uuid",
    "productId": "product-uuid",
    "productName": "Pro Pack",
    "fileName": "pack.zip",
    "downloadToken": "tok-uuid",
    "downloadCount": 0,
    "maxDownloads": 5,
    "remainingDownloads": 5,
    "expiresAt": "2027-05-01T00:00:00Z",
    "active": true,
    "grantedByOrder": "order-uuid",
    "grantedBySubscription": null
  }
]
```

**`GET /api/v1/library/{token}/download`**

- Requires ownership of the entitlement.
- Rejects expired/exhausted downloads.
- Increments the download count and returns the asset URL and remaining count.

Subscription entitlements have a higher download limit (50) and expire at the end of the current
paid period. Canceled/PAST_DUE/expired subscriptions no longer expose active downloads; one-off
order entitlements allow 5 downloads and expire after one year. Order cancellation/full refund
revokes order-based entitlements immediately.
