# Funnel & Attribution Analytics

Anonymous storefront events power conversion funnels, UTM/referrer attribution and per-product
conversion reporting.

**Base URL:** `http://localhost:8080`

## Event ingestion

**`POST /api/v1/analytics/events`** — public, rate-limited, max 50 events per request.

```json
{
  "events": [
    {
      "eventType": "PRODUCT_VIEW",
      "sessionId": "sess_123",
      "productId": "product-uuid",
      "path": "/products/phone",
      "referrer": "https://google.com",
      "utmSource": "google",
      "utmMedium": "cpc",
      "utmCampaign": "spring",
      "locale": "en",
      "occurredAt": "2026-05-01T10:00:00"
    }
  ]
}
```

Event types:

- `PAGE_VIEW`
- `PRODUCT_VIEW`
- `SEARCH`
- `ADD_TO_CART`
- `CHECKOUT_START`
- `PURCHASE`
- `SIGNUP`

`sessions` are distinct `sessionId` values, so multiple events from one visitor count once per
funnel stage.

## Funnel

**`GET /api/v1/admin/analytics/funnel?startDate=2026-05-01&endDate=2026-05-31`**

Stages: `page_view` → `product_view` → `add_to_cart` → `checkout_start` → `purchase`.

```json
{
  "startDate": "2026-05-01",
  "endDate": "2026-05-31",
  "stages": [
    { "name": "page_view", "sessions": 1000, "conversionFromPrevious": null },
    { "name": "product_view", "sessions": 420, "conversionFromPrevious": 42.0 },
    { "name": "add_to_cart", "sessions": 180, "conversionFromPrevious": 42.86 },
    { "name": "checkout_start", "sessions": 90, "conversionFromPrevious": 50.0 },
    { "name": "purchase", "sessions": 45, "conversionFromPrevious": 50.0 }
  ],
  "overallConversion": 4.5
}
```

`purchase` counts paid, non-canceled orders in the range.

## Attribution

**`GET /api/v1/admin/analytics/attribution?startDate=...&endDate=...`**

Paid orders are grouped by the first-touch attribution captured at checkout. Orders without
attribution are grouped under `direct` / `none` / `none`.

```json
{
  "startDate": "2026-05-01",
  "endDate": "2026-05-31",
  "rows": [
    {
      "source": "google",
      "medium": "cpc",
      "campaign": "spring",
      "orders": 12,
      "revenue": "1440.00",
      "revenueUsd": "1200.00"
    }
  ],
  "totalOrders": 12,
  "totalRevenueUsd": "1200.00"
}
```

Revenue is normalized to USD via the order FX rate.

## Product conversion

**`GET /api/v1/admin/analytics/products?startDate=...&endDate=...&limit=20`**

Per product:

- `views` — distinct `PRODUCT_VIEW` events
- `addToCart` — distinct `ADD_TO_CART` events
- `purchases` — units in paid orders
- `conversionRate` — purchases / views
- `revenue` — paid-order item revenue (USD)

## Checkout attribution

`CheckoutRequest` accepts an optional first-touch attribution block:

```json
{
  "attribution": {
    "sessionId": "sess_123",
    "source": "google",
    "medium": "cpc",
    "campaign": "spring",
    "referrer": "https://google.com"
  }
}
```

The values are stored on the order and returned as `attributionSource`,
`attributionMedium` and `attributionCampaign`.
