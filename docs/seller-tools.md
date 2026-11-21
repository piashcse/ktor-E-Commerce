# Seller Reviews, Disputes & Commission Rules

Marketplace trust and economics: post-delivery seller ratings, buyer/seller disputes and
scope-aware commission rules.

## Seller reviews

Customers can review a seller once per order after it is `DELIVERED` or `RECEIVED`.

**`POST /api/v1/seller-reviews`**
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
| `GET` | `/api/v1/admin/disputes` | Admin: list disputes |
| `PUT` | `/api/v1/admin/disputes/{id}` | Admin: resolve/update a dispute |
| `GET` | `/api/v1/disputes/{id}` | Get a dispute with its message thread |
| `POST` | `/api/v1/disputes/{id}/messages` | Add a message or evidence to a dispute |
| `GET` | `/api/v1/seller-reviews/mine` | List my seller reviews |
| `GET` | `/api/v1/seller/commission` | Seller: get my effective commission rate |
| `GET` | `/api/v1/seller/commission/orders/{orderId}` | Seller: per-order commission and payout preview |
| `POST` | `/api/v1/seller/reviews/products/{id}/reply` | Seller: reply to a product review |
| `POST` | `/api/v1/seller/reviews/{id}/reply` | Seller: reply to a seller review |

Moderation body:

```json
{ "status": "HIDDEN" }
```

Only `ACTIVE` reviews count toward the shop's `rating` and `totalReviews`.

### 1. Disputes

**`POST /api/v1/disputes`**
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
## Endpoint Details

### 2. List active reviews for a seller

**`GET /api/v1/sellers/{id}/reviews`**

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/sellers/6c5078d3-f8e3-4c88-9afe-48b5423c664f/reviews?page=1&perPage=20' \
  -H 'accept: application/json'
```

#### Response

```json
{
  "data": [
    {
      "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "sellerId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "shopId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "userId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "orderId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "rating": 1,
      "comment": "Looks good",
      "status": "ACTIVE",
      "sellerReply": "Looks good",
      "sellerRepliedAt": "2024-05-06T12:00:00",
      "moderationReason": "Policy violation",
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

### 3. Seller: list reviews received

**`GET /api/v1/seller/reviews`**

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/seller/reviews?page=1&perPage=20' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <seller_token>'
```

#### Response

```json
{
  "data": [
    {
      "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "sellerId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "shopId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "userId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "orderId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "rating": 1,
      "comment": "Looks good",
      "status": "ACTIVE",
      "sellerReply": "Looks good",
      "sellerRepliedAt": "2024-05-06T12:00:00",
      "moderationReason": "Policy violation",
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

### 4. Admin: list seller reviews

**`GET /api/v1/admin/seller-reviews`**

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/admin/seller-reviews?page=1&perPage=20&status=PENDING' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <admin_token>'
```

#### Response

```json
{
  "data": [
    {
      "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "sellerId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "shopId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "userId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "orderId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "rating": 1,
      "comment": "Looks good",
      "status": "ACTIVE",
      "sellerReply": "Looks good",
      "sellerRepliedAt": "2024-05-06T12:00:00",
      "moderationReason": "Policy violation",
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

### 5. Admin: moderate a seller review

**`PUT /api/v1/admin/seller-reviews/{id}/status`**

```bash
curl -X 'PUT' \
  'http://localhost:8080/api/v1/admin/seller-reviews/6c5078d3-f8e3-4c88-9afe-48b5423c664f/status' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <admin_token>' \
  -H 'Content-Type: application/json' \
  -d '{
  "status": "ACTIVE",
  "reason": "Policy violation"
}'
```

#### Response

```json
{
  "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "sellerId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "shopId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "userId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "orderId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "rating": 1,
  "comment": "Looks good",
  "status": "ACTIVE",
  "sellerReply": "Looks good",
  "sellerRepliedAt": "2024-05-06T12:00:00",
  "moderationReason": "Policy violation",
  "createdAt": "2024-05-06T12:00:00"
}
```

---

### 6. Admin: list disputes

**`GET /api/v1/admin/disputes`**

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/admin/disputes?page=1&perPage=20&status=PENDING' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <admin_token>'
```

#### Response

```json
{
  "data": [
    {
      "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "orderId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "sellerId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "userId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "type": "ORDER_ISSUE",
      "subject": "Looks good",
      "description": "Looks good",
      "status": "OPEN",
      "resolution": "Policy violation",
      "resolvedBy": "admin@example.com",
      "resolvedAt": "2024-05-06T12:00:00",
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

### 7. Admin: resolve/update a dispute

**`PUT /api/v1/admin/disputes/{id}`**

```bash
curl -X 'PUT' \
  'http://localhost:8080/api/v1/admin/disputes/6c5078d3-f8e3-4c88-9afe-48b5423c664f' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <admin_token>' \
  -H 'Content-Type: application/json' \
  -d '{
  "status": "OPEN",
  "resolution": "Policy violation"
}'
```

#### Response

```json
{
  "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "orderId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "sellerId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "userId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "type": "ORDER_ISSUE",
  "subject": "Looks good",
  "description": "Looks good",
  "status": "OPEN",
  "resolution": "Policy violation",
  "resolvedBy": "admin@example.com",
  "resolvedAt": "2024-05-06T12:00:00",
  "createdAt": "2024-05-06T12:00:00"
}
```

---

### 8. Get a dispute with its message thread

**`GET /api/v1/disputes/{id}`**

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/disputes/6c5078d3-f8e3-4c88-9afe-48b5423c664f' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <access_token>'
```

#### Response

```json
{
  "dispute": {
    "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
    "orderId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
    "sellerId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
    "userId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
    "type": "ORDER_ISSUE",
    "subject": "Looks good",
    "description": "Looks good",
    "status": "OPEN",
    "resolution": "Policy violation",
    "resolvedBy": "admin@example.com",
    "resolvedAt": "2024-05-06T12:00:00",
    "createdAt": "2024-05-06T12:00:00"
  },
  "messages": [
    {
      "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "authorId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "body": "Looks good",
      "isInternal": true,
      "createdAt": "2024-05-06T12:00:00"
    }
  ]
}
```

---

### 9. Add a message or evidence to a dispute

**`POST /api/v1/disputes/{id}/messages`**

```bash
curl -X 'POST' \
  'http://localhost:8080/api/v1/disputes/6c5078d3-f8e3-4c88-9afe-48b5423c664f/messages' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <access_token>' \
  -H 'Content-Type: application/json' \
  -d '{
  "body": "Looks good",
  "isInternal": true
}'
```

#### Response

```json
{
  "dispute": {
    "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
    "orderId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
    "sellerId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
    "userId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
    "type": "ORDER_ISSUE",
    "subject": "Looks good",
    "description": "Looks good",
    "status": "OPEN",
    "resolution": "Policy violation",
    "resolvedBy": "admin@example.com",
    "resolvedAt": "2024-05-06T12:00:00",
    "createdAt": "2024-05-06T12:00:00"
  },
  "messages": [
    {
      "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "authorId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "body": "Looks good",
      "isInternal": true,
      "createdAt": "2024-05-06T12:00:00"
    }
  ]
}
```

---

### 10. List my seller reviews

**`GET /api/v1/seller-reviews/mine`**

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/seller-reviews/mine?page=1&perPage=20' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <access_token>'
```

#### Response

```json
{
  "data": [
    {
      "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "sellerId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "shopId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "userId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "orderId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "rating": 1,
      "comment": "Looks good",
      "status": "ACTIVE",
      "sellerReply": "Looks good",
      "sellerRepliedAt": "2024-05-06T12:00:00",
      "moderationReason": "Policy violation",
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

### 11. Seller: get my effective commission rate

**`GET /api/v1/seller/commission`**

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/seller/commission' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <seller_token>'
```

#### Response

```json
{
  "rate": "250.00",
  "source": "google"
}
```

---

### 12. Seller: per-order commission and payout preview

**`GET /api/v1/seller/commission/orders/{orderId}`**

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/seller/commission/orders/ORD-20240506-0001-A1B2C3D4' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <seller_token>'
```

#### Response

```json
{
  "orderId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "orderNumber": "ORD-20240506-0001-A1B2C3D4",
  "subTotal": "250.00",
  "discountAmount": "250.00",
  "commissionAmount": "250.00",
  "payoutAmount": "250.00",
  "commissionRate": "250.00",
  "currency": "USD"
}
```

---

### 13. Seller: reply to a product review

**`POST /api/v1/seller/reviews/products/{id}/reply`**

```bash
curl -X 'POST' \
  'http://localhost:8080/api/v1/seller/reviews/products/6c5078d3-f8e3-4c88-9afe-48b5423c664f/reply' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <seller_token>' \
  -H 'Content-Type: application/json' \
  -d '{
  "reply": "Looks good"
}'
```

#### Response

```json
{
  "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "userId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "productId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "reviewText": "Looks good",
  "rating": 1,
  "title": "Example",
  "isVerifiedPurchase": true,
  "helpfulCount": 1,
  "notHelpfulCount": 1,
  "images": [
    "https://example.com/image.jpg"
  ],
  "status": "ACTIVE",
  "sellerReply": "Looks good",
  "sellerRepliedAt": "2024-05-06T12:00:00",
  "moderationReason": "Policy violation",
  "createdAt": "2024-05-06T12:00:00"
}
```

---

### 14. Seller: reply to a seller review

**`POST /api/v1/seller/reviews/{id}/reply`**

```bash
curl -X 'POST' \
  'http://localhost:8080/api/v1/seller/reviews/6c5078d3-f8e3-4c88-9afe-48b5423c664f/reply' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <seller_token>' \
  -H 'Content-Type: application/json' \
  -d '{
  "reply": "Looks good"
}'
```

#### Response

```json
{
  "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "sellerId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "shopId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "userId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "orderId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "rating": 1,
  "comment": "Looks good",
  "status": "ACTIVE",
  "sellerReply": "Looks good",
  "sellerRepliedAt": "2024-05-06T12:00:00",
  "moderationReason": "Policy violation",
  "createdAt": "2024-05-06T12:00:00"
}
```

---
