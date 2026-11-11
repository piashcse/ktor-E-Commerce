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
  "paymentReference": "pay_123",
  "paymentMethodReference": "pm_123",
  "providerCustomerReference": "cus_123"
}
```

- With a `paymentReference`, creates an `ACTIVE` subscription with a paid invoice.
- Without a reference, creates a `PENDING` subscription and a `PENDING` invoice; no entitlement is granted until payment is recorded.
- `paymentMethodReference` / `providerCustomerReference` are **provider tokens**, not card data. They
  enable off-session renewal charges with `PAYMENT_PROVIDER=stripe`. Never send raw card numbers.
- Period starts now and ends after one month/year (plus trial days).
- Grants a digital entitlement when the plan’s product is digital and payment is confirmed.
- Rejects duplicate active subscriptions for the same plan.

Other endpoints:

- `GET /api/v1/subscriptions`
- `GET /api/v1/subscriptions/{id}/invoices`
- `POST /api/v1/subscriptions/{id}/cancel` — marks `cancelAtPeriodEnd`

## Renewal billing

**`POST /api/v1/admin/subscriptions/run-billing`**

Processes every `ACTIVE` subscription whose period has ended and every `PAST_DUE` subscription
that is due for a retry:

- `cancelAtPeriodEnd = true` → status becomes `CANCELED`, entitlement revoked
- with a stored payment method and `PAYMENT_PROVIDER=stripe` → charges the saved payment method
  off-session. A successful charge extends the period, records a `PAID` invoice, resets dunning and
  refreshes the entitlement. A `requires_action`/`processing` charge stays pending.
- without a payment method (or with the manual provider) → creates/keeps a `PENDING` invoice and
  sets the subscription `PAST_DUE` for manual collection.
- failed charges record the attempt, failure reason and next retry, and keep the subscription
  `PAST_DUE` during the 3-day grace period. After the configured attempts/grace period it becomes
  `EXPIRED` and digital access is revoked.

Returns:

```json
{ "invoicesCreated": 1, "canceled": 0, "paid": 1, "failed": 0, "expired": 0 }
```

Provider renewal webhooks (Stripe `payment_intent.*`) are matched by the
`subscription_invoice_id` metadata and applied idempotently; a successful webhook also marks the
invoice paid and activates the subscription.

Confirm a renewal payment manually and reactivate:

**`POST /api/v1/admin/subscriptions/{id}/activate`**

```json
{ "paymentReference": "pay_renewal" }
```

Marks the pending/failed invoice paid, advances the period, sets the subscription `ACTIVE`, resets
dunning, and refreshes the digital entitlement expiry.

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
- Increments the download count and returns a short-lived signed
  `/api/v1/library/asset?...` URL plus its expiry and remaining count.
- The raw asset URL is never returned in JSON; the signed endpoint validates the HMAC/expiry and
  redirects to the asset.

Subscription entitlements have a higher download limit (50) and expire at the end of the current
paid period. Canceled/PAST_DUE/expired subscriptions no longer expose active downloads and their
entitlements are removed on cancellation/expiry; one-off
order entitlements allow 5 downloads and expire after one year. Order cancellation/full refund
revokes order-based entitlements immediately.
