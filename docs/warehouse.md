# Warehouses

Sellers can split inventory across physical locations. Every shop receives a `DEFAULT`
warehouse automatically; existing shops were backfilled by V24.

## Endpoints

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/v1/seller/warehouses` | List active warehouses |
| `POST` | `/api/v1/seller/warehouses` | Create a warehouse |
| `PUT` | `/api/v1/seller/warehouses/{id}` | Update a warehouse |
| `DELETE` | `/api/v1/seller/warehouses/{id}` | Deactivate a warehouse |

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

**`GET /api/v1/seller/warehouses`**

List warehouses

#### Request

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

**`POST /api/v1/seller/warehouses`**

Create warehouse

#### Request

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

**`PUT /api/v1/seller/warehouses/{id}`**

Update warehouse

#### Request

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

**`DELETE /api/v1/seller/warehouses/{id}`**

Deactivate warehouse

#### Request

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
