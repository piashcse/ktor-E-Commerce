# Recommendations

Recommendations are read-only, cache-friendly and derived from catalog signals that already exist in the database.

## Public endpoints

| Method | Endpoint | Strategy |
|---|---|---|
| `GET` | `/api/v1/products/recommendations?limit=10` | Best sellers (guest-safe fallback) |
| `GET` | `/api/v1/products/{productId}/related?limit=10` | Same category, subcategory boosted |
| `GET` | `/api/v1/products/{productId}/also-bought?limit=10` | Co-purchase from paid, non-canceled orders |
| `GET` | `/api/v1/recommendations?limit=10` | Personalized feed (customer auth required) |

## Personalized endpoint

**`GET /api/v1/recommendations?limit=10`** — customer auth required.

Uses the customer's order history, cart and wishlist categories as weighted signals, excludes items they already bought or have in the cart/wishlist, and fills any shortfall with best sellers.

## Response

```json
{
  "strategy": "also-bought",
  "sourceProductId": "product-uuid",
  "items": [
    {
      "id": "recommended-product-uuid",
      "name": "Matching Product",
      "price": "19.99",
      "images": ["https://example.com/image.jpg"],
      "status": "ACTIVE",
      "totalSales": 120,
      "rating": 4.8
    }
  ]
}
```

Strategies returned: `personalized`, `related`, `also-bought`, `best-sellers`.

## Ranking and safety

- `also-bought` counts distinct co-purchase occurrences across paid, non-canceled orders.
- `related` prioritizes same subcategory, then sales and rating.
- Only `ACTIVE`, non-deleted products from `APPROVED` shops are returned.
- `limit` is clamped to `1..50` (default `10`).
- Results are cached for the cache TTL (default 5 minutes).
