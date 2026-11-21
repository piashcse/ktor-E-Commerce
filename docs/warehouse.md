# Warehouses

Split seller inventory across physical warehouse locations.

## Endpoints

| Method | Endpoint | Description |
|---|---|---|
| <span class="method-badge get">GET</span> | `/api/v1/seller/warehouses` | List active warehouses |
| <span class="method-badge post">POST</span> | `/api/v1/seller/warehouses` | Create a warehouse |
| <span class="method-badge put">PUT</span> | `/api/v1/seller/warehouses/{id}` | Update a warehouse |
| <span class="method-badge delete">DELETE</span> | `/api/v1/seller/warehouses/{id}` | Deactivate a warehouse |

Body:

```json
{
  "name": "Dhaka DC",
  "code": "DAC",
  "address": "12 Industrial Rd",
  "city": "Dhaka",
  "country": "BD"
}
```

Warehouse `code` is unique per shop. Inventory rows are keyed by
`(product, shop, warehouse, variant)`; order placement allocates stock oldest warehouse first.
Deactivation keeps historical rows but prevents new allocations.

See [Inventory](inventory.md) and [Product Variants](product-variants.md).
## Endpoint Details

### 1. List warehouses

<div class="endpoint-header">
  <span class="method-badge get">GET</span>
  <span class="endpoint-path">/api/v1/seller/warehouses</span>
</div>

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/seller/warehouses' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <seller_token>'
```

#### Response

```json
[
  {
    "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
    "shopId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
    "name": "Example",
    "code": "SAVE10",
    "address": "123 Main St",
    "city": "New York",
    "country": "US",
    "isActive": true,
    "latitude": "string",
    "longitude": "string",
    "phone": "+15551234567",
    "openingHours": "string",
    "supportsPickup": true,
    "supportsPos": true,
    "createdAt": "2024-05-06T12:00:00"
  }
]
```

---

### 2. Create warehouse

<div class="endpoint-header">
  <span class="method-badge post">POST</span>
  <span class="endpoint-path">/api/v1/seller/warehouses</span>
</div>

```bash
curl -X 'POST' \
  'http://localhost:8080/api/v1/seller/warehouses' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <seller_token>' \
  -H 'Content-Type: application/json' \
  -d '{
  "name": "Example",
  "code": "SAVE10",
  "address": "123 Main St",
  "city": "New York",
  "country": "US",
  "latitude": "string",
  "longitude": "string",
  "phone": "+15551234567",
  "openingHours": "string",
  "supportsPickup": true,
  "supportsPos": true
}'
```

#### Response

```json
{
  "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "shopId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "name": "Example",
  "code": "SAVE10",
  "address": "123 Main St",
  "city": "New York",
  "country": "US",
  "isActive": true,
  "latitude": "string",
  "longitude": "string",
  "phone": "+15551234567",
  "openingHours": "string",
  "supportsPickup": true,
  "supportsPos": true,
  "createdAt": "2024-05-06T12:00:00"
}
```

---

### 3. Update warehouse

<div class="endpoint-header">
  <span class="method-badge put">PUT</span>
  <span class="endpoint-path">/api/v1/seller/warehouses/{id}</span>
</div>

```bash
curl -X 'PUT' \
  'http://localhost:8080/api/v1/seller/warehouses/6c5078d3-f8e3-4c88-9afe-48b5423c664f' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <seller_token>' \
  -H 'Content-Type: application/json' \
  -d '{
  "name": "Example",
  "code": "SAVE10",
  "address": "123 Main St",
  "city": "New York",
  "country": "US",
  "latitude": "string",
  "longitude": "string",
  "phone": "+15551234567",
  "openingHours": "string",
  "supportsPickup": true,
  "supportsPos": true
}'
```

#### Response

```json
{
  "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "shopId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "name": "Example",
  "code": "SAVE10",
  "address": "123 Main St",
  "city": "New York",
  "country": "US",
  "isActive": true,
  "latitude": "string",
  "longitude": "string",
  "phone": "+15551234567",
  "openingHours": "string",
  "supportsPickup": true,
  "supportsPos": true,
  "createdAt": "2024-05-06T12:00:00"
}
```

---

### 4. Deactivate warehouse

<div class="endpoint-header">
  <span class="method-badge delete">DELETE</span>
  <span class="endpoint-path">/api/v1/seller/warehouses/{id}</span>
</div>

```bash
curl -X 'DELETE' \
  'http://localhost:8080/api/v1/seller/warehouses/6c5078d3-f8e3-4c88-9afe-48b5423c664f' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <seller_token>'
```

#### Response

```json
{
  "message": "Operation completed"
}
```

---
