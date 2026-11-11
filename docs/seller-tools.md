# Seller Reviews, Disputes & Commission Rules

Marketplace trust and economics: post-delivery seller ratings, buyer/seller disputes and
scope-aware commission rules.

**Base URL:** `http://localhost:8080`

## Seller reviews

Customers can review a seller once per order after it is `DELIVERED` or `RECEIVED`.

**`POST /api/v1/seller-reviews`**

```json
{
  "orderId": "order-uuid",
  "rating": 5,
  "comment": "Fast shipping and great packaging"
}
```

- Rating must be `1..5`.
- The order must belong to the caller.
- The seller is derived from the order's shop.
- One review per order.

Admin moderation:

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/v1/sellers/{id}/reviews` | Public active reviews |
| `GET` | `/api/v1/seller/reviews` | Seller's received reviews |
| `GET` | `/api/v1/admin/seller-reviews?status=` | Admin moderation list |
| `PUT` | `/api/v1/admin/seller-reviews/{id}/status` | Hide/flag/activate |

Moderation body:

```json
{ "status": "HIDDEN" }
```

Only `ACTIVE` reviews count toward the shop's `rating` and `totalReviews`.

## Disputes

**`POST /api/v1/disputes`**

```json
{
  "orderId": "order-uuid",
  "type": "ORDER_ISSUE",
  "subject": "Missing item",
  "description": "One item was missing from the parcel"
}
```

Types: `ORDER_ISSUE`, `PAYOUT`, `REVIEW`, `OTHER`.

- A linked order is only accessible by its customer, its shop's seller, or an admin.
- Without an order, the caller's seller profile (if any) is attached.
- `GET /api/v1/disputes` returns disputes where the caller is the customer or the seller.

Admin lifecycle:

| Status | Allowed next |
|---|---|
| `OPEN` | `UNDER_REVIEW`, `RESOLVED`, `REJECTED` |
| `UNDER_REVIEW` | `RESOLVED`, `REJECTED` |
| `RESOLVED` / `REJECTED` | terminal |

```json
{ "status": "RESOLVED", "resolution": "Replacement sent" }
```

Resolution stores the admin user and timestamp.

## Commission rules

Rules are matched by scope; **seller (4) > shop (2) > category (1)**, then highest `priority`,
then oldest rule. If no rule matches, the seller's `commissionRate` is used.

```json
{
  "name": "Electronics seller rate",
  "sellerId": "seller-uuid",
  "shopId": null,
  "categoryId": null,
  "rate": 3.00,
  "priority": 10,
  "isActive": true
}
```

`rate` is a percentage (`3.00` = 3%) and must be `0..100`.

Admin endpoints:

- `GET /api/v1/admin/commission-rules`
- `POST /api/v1/admin/commission-rules`
- `PUT /api/v1/admin/commission-rules/{id}`
- `DELETE /api/v1/admin/commission-rules/{id}`

Seller view:

```bash
curl 'http://localhost:8080/api/v1/seller/commission?categoryId=category-uuid' \
  -H 'Authorization: Bearer <seller-token>'
```

```json
{ "rate": "3.00", "source": "rule" }
```

### Payout integration

Seller payouts now compute commission per order line using the resolved category/seller/shop rule
(weighted by each line's net amount) instead of always using `seller.commissionRate`. Refund
commission reversals use the same resolver for the refunded item's category.
