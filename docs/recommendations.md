# Recommendations

Read-only product recommendations from catalog and purchase signals.

## Public endpoints

| Method | Endpoint | Strategy |
|---|---|---|
| <span class="method-badge get">GET</span> | `/api/v1/products/recommendations?limit=10` | Best sellers (guest-safe fallback) |
| <span class="method-badge get">GET</span> | `/api/v1/products/{productId}/related?limit=10` | Same category, subcategory boosted |
| <span class="method-badge get">GET</span> | `/api/v1/products/{productId}/also-bought?limit=10` | Co-purchase from paid, non-canceled orders |
| <span class="method-badge get">GET</span> | `/api/v1/recommendations?limit=10` | Personalized feed (customer auth required) |

### 1. Personalized endpoint

<div class="endpoint-header">
  <span class="method-badge get">GET</span>
  <span class="endpoint-path">/api/v1/recommendations?limit=10</span>
</div>

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
## Endpoint Details

### 2. Best-seller recommendations

<div class="endpoint-header">
  <span class="method-badge get">GET</span>
  <span class="endpoint-path">/api/v1/products/recommendations</span>
</div>

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/products/recommendations' \
  -H 'accept: application/json'
```

#### Response

```json
{
  "strategy": "250.00",
  "sourceProductId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "items": [
    {
      "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "userId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "shopId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "categoryId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "subCategoryId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "brandId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "name": "Example",
      "description": "Looks good",
      "sku": "SKU-12345",
      "barcode": "SAVE10",
      "weight": 250.0,
      "dimensions": "10x20x30 cm",
      "minOrderQuantity": 1,
      "price": "250.00",
      "discountPrice": "250.00",
      "discountPercentage": 250.0,
      "videoLink": "https://example.com/video.mp4",
      "hotDeal": true,
      "featured": true,
      "bestSeller": true
    }
  ]
}
```

---

### 3. Related products

<div class="endpoint-header">
  <span class="method-badge get">GET</span>
  <span class="endpoint-path">/api/v1/products/{id}/related</span>
</div>

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/products/6c5078d3-f8e3-4c88-9afe-48b5423c664f/related' \
  -H 'accept: application/json'
```

#### Response

```json
{
  "strategy": "250.00",
  "sourceProductId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "items": [
    {
      "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "userId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "shopId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "categoryId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "subCategoryId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "brandId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "name": "Example",
      "description": "Looks good",
      "sku": "SKU-12345",
      "barcode": "SAVE10",
      "weight": 250.0,
      "dimensions": "10x20x30 cm",
      "minOrderQuantity": 1,
      "price": "250.00",
      "discountPrice": "250.00",
      "discountPercentage": 250.0,
      "videoLink": "https://example.com/video.mp4",
      "hotDeal": true,
      "featured": true,
      "bestSeller": true
    }
  ]
}
```

---

### 4. Frequently bought together

<div class="endpoint-header">
  <span class="method-badge get">GET</span>
  <span class="endpoint-path">/api/v1/products/{id}/also-bought</span>
</div>

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/products/6c5078d3-f8e3-4c88-9afe-48b5423c664f/also-bought' \
  -H 'accept: application/json'
```

#### Response

```json
{
  "strategy": "250.00",
  "sourceProductId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "items": [
    {
      "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "userId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "shopId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "categoryId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "subCategoryId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "brandId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "name": "Example",
      "description": "Looks good",
      "sku": "SKU-12345",
      "barcode": "SAVE10",
      "weight": 250.0,
      "dimensions": "10x20x30 cm",
      "minOrderQuantity": 1,
      "price": "250.00",
      "discountPrice": "250.00",
      "discountPercentage": 250.0,
      "videoLink": "https://example.com/video.mp4",
      "hotDeal": true,
      "featured": true,
      "bestSeller": true
    }
  ]
}
```

---
