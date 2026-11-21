# Inventory

Per-shop and per-warehouse stock, low-stock alerts and adjustments.

## Authentication

All Inventory endpoints require Bearer token authentication for sellers and admins.

### Inventory Endpoints

| Method | Endpoint | Description | Authentication Required |
|--------|----------|-------------|------------------------|
| <span class="method-badge post">POST</span> | `/api/v1/seller/inventories` | Create a new inventory record | Yes (Seller) |
| <span class="method-badge put">PUT</span> | `/api/v1/seller/inventories/stock/{productId}` | Update product stock quantity | Yes (Seller) |
| <span class="method-badge get">GET</span> | `/api/v1/seller/inventories/product/{productId}` | Retrieve inventory item details by product ID | Yes (Seller) |
| <span class="method-badge get">GET</span> | `/api/v1/seller/inventories/shop/{shopId}` | Retrieve all inventory items for a shop | Yes (Seller) |
| <span class="method-badge get">GET</span> | `/api/v1/seller/inventories/low-stock` | Retrieve items with stock below a threshold | Yes (Seller) |
| <span class="method-badge get">GET</span> | `/api/v1/admin/inventories` | List inventory across all shops | Yes (Admin) |

---

## Endpoint Details

### 1. Update Product Stock (Seller)

<div class="endpoint-header">
  <span class="method-badge put">PUT</span>
  <span class="endpoint-path">/api/v1/seller/inventories/stock/{productId}</span>
</div>

```bash
curl -X 'PUT' \
  'http://localhost:8080/api/v1/seller/inventories/stock/cbd630f6-bf9f-48ad-ac51-f806807d99fd?quantity=50&operation=add' \
  -H 'Authorization: Bearer <seller_token>'
```

#### Response

```json
{
    "id": "cbd630f6-bf9f-48ad-ac51-f806807d99fd",
    "stockQuantity": 250,
    "status": "IN_STOCK"
}
```

---

### 2. Get Low Stock Records

<div class="endpoint-header">
  <span class="method-badge get">GET</span>
  <span class="endpoint-path">/api/v1/seller/inventories/low-stock</span>
</div>

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/seller/inventories/low-stock' \
  -H 'Authorization: Bearer <seller_token>'
```

### 3. Admin inventory

<div class="endpoint-header">
  <span class="method-badge get">GET</span>
  <span class="endpoint-path">/api/v1/admin/inventories</span>
</div>

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/admin/inventories?page=1&perPage=20' \
  -H 'Authorization: Bearer <admin_token>'
```

---

### 4. Create or update inventory

<div class="endpoint-header">
  <span class="method-badge post">POST</span>
  <span class="endpoint-path">/api/v1/seller/inventories</span>
</div>

#### Request

```bash
curl -X 'POST' \
  'http://localhost:8080/api/v1/seller/inventories' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <seller_token>' \
  -H 'Content-Type: application/json' \
  -d '{
  "productId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "shopId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "variantId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "warehouseId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "stockQuantity": 1,
  "reservedQuantity": 1,
  "minimumStockLevel": 1,
  "maximumStockLevel": 1
}'
```

#### Response

```json
{
  "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "productId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "productName": "Wireless Headphones",
  "sku": "SKU-12345",
  "variantId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "shopId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "warehouseId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "stockQuantity": 1,
  "reservedQuantity": 1,
  "minimumStockLevel": 1,
  "maximumStockLevel": 1,
  "status": "IN_STOCK",
  "lastRestocked": "string",
  "createdAt": "2024-05-06T12:00:00",
  "updatedAt": "2024-05-06T12:00:00"
}
```

---

### 5. Get product inventory

<div class="endpoint-header">
  <span class="method-badge get">GET</span>
  <span class="endpoint-path">/api/v1/seller/inventories/product/{productId}</span>
</div>

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/seller/inventories/product/6c5078d3-f8e3-4c88-9afe-48b5423c664f' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <seller_token>'
```

#### Response

```json
{
  "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "productId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "productName": "Wireless Headphones",
  "sku": "SKU-12345",
  "variantId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "shopId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "warehouseId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "stockQuantity": 1,
  "reservedQuantity": 1,
  "minimumStockLevel": 1,
  "maximumStockLevel": 1,
  "status": "IN_STOCK",
  "lastRestocked": "string",
  "createdAt": "2024-05-06T12:00:00",
  "updatedAt": "2024-05-06T12:00:00"
}
```

---

### 6. List shop inventory

<div class="endpoint-header">
  <span class="method-badge get">GET</span>
  <span class="endpoint-path">/api/v1/seller/inventories/shop/{shopId}</span>
</div>

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/seller/inventories/shop/6c5078d3-f8e3-4c88-9afe-48b5423c664f?page=1&perPage=20&q=phone' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <seller_token>'
```

#### Response

```json
{
  "data": [
    {
      "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "productId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "productName": "Wireless Headphones",
      "sku": "SKU-12345",
      "variantId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "shopId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "warehouseId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "stockQuantity": 1,
      "reservedQuantity": 1,
      "minimumStockLevel": 1,
      "maximumStockLevel": 1,
      "status": "IN_STOCK",
      "lastRestocked": "string",
      "createdAt": "2024-05-06T12:00:00",
      "updatedAt": "2024-05-06T12:00:00"
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

## Error Handling

| Status Code | Description |
|-------------|-------------|
| `400` | Bad Request |
| `401` | Unauthorized |
| `403` | Forbidden (User does not own the product) |
| `404` | Not Found |

## Multi-warehouse

Sellers manage physical locations at `/api/v1/seller/warehouses` (create/list/update/deactivate). Inventory rows are per `(product, shop, warehouse)`; `stockQuantity` is tracked per warehouse, and order placement allocates stock across warehouses oldest-first. Existing shops receive a `DEFAULT` warehouse via V24.

## Variants

Inventory requests accept an optional `variantId`. Variant rows are unique per `(product, shop, warehouse, variant)`, while `variantId = NULL` keeps legacy product-level stock. When updating stock or reading product inventory for a variant, pass `?variantId=...`:

```bash
curl -X 'PUT' \
  'http://localhost:8080/api/v1/seller/inventories/stock/{productId}?quantity=10&operation=add&variantId={variantId}' \
  -H 'Authorization: Bearer <seller_token>'
```

See [Product Variants](product-variants.md).
