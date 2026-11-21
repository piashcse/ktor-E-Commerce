# Product Variants & Options

Products can expose purchasable variants (e.g. `T-Shirt / Red / M`) with their own SKU, price, image and stock. Variants are optional; products without variants keep the existing product-level price and inventory.

## Public endpoints

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/v1/products/{productId}/variants` | List purchasable variants |
| `GET` | `/api/v1/products/{productId}/options` | List option dimensions and values |

Product detail (`GET /api/v1/products/{productId}`) also includes a `variants` array.

Variant response:

```json
{
  "id": "variant-uuid",
  "productId": "product-uuid",
  "sku": "TSHIRT-RED-M",
  "name": "Red / M",
  "price": "19.99",
  "discountPrice": "15.99",
  "status": "ACTIVE",
  "isDefault": true,
  "imageUrl": "https://example.com/red-m.jpg",
  "options": [
    { "optionId": "color-id", "optionName": "Color", "valueId": "red-id", "value": "Red" },
    { "optionId": "size-id", "optionName": "Size", "valueId": "m-id", "value": "M" }
  ]
}
```

## Seller endpoints

All routes are under `/api/v1/seller/products` and require the seller to own the product.

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/v1/seller/products/{productId}/variants` | Create a variant |
| `PUT` | `/api/v1/seller/products/{productId}/variants/{variantId}` | Update a variant |
| `DELETE` | `/api/v1/seller/products/{productId}/variants/{variantId}` | Delete a variant |
| `PUT` | `/api/v1/seller/products/{productId}/variants/{variantId}/options` | Replace attached option values |
| `POST` | `/api/v1/seller/products/{productId}/options` | Create an option dimension, e.g. `Size` |
| `DELETE` | `/api/v1/seller/products/{productId}/options/{optionId}` | Delete an option and its values |
| `POST` | `/api/v1/seller/products/{productId}/options/{optionId}/values` | Add a value, e.g. `M` |
| `DELETE` | `/api/v1/seller/products/{productId}/options/{optionId}/values/{valueId}` | Delete a value |

Create variant body:

```json
{
  "sku": "TSHIRT-RED-M",
  "name": "Red / M",
  "price": 19.99,
  "discountPrice": 15.99,
  "imageUrl": "https://example.com/red-m.jpg",
  "status": "ACTIVE",
  "isDefault": true,
  "sortOrder": 0
}
```

Attach option values (one value per option):

```json
{ "valueIds": ["red-id", "m-id"] }
```

## Cart and checkout

- When a product has variants, `POST /api/v1/carts` requires `variantId`; adding the base product is rejected.
- Cart lines are unique per `(user, product, variant)`, so several variants of one product can coexist.
- Checkout uses the variant price, SKU, name and variant-level stock; order items snapshot `variantId` and `variantName`.
- Cancellations, refunds and RMA replacements restore stock to the purchased variant.

## Inventory

Inventory rows accept an optional `variantId`. Rows with `variantId = NULL` are legacy product-level stock; variant rows are tracked per `(product, shop, warehouse, variant)`. See [Inventory](inventory.md).
## Endpoint Details

### 1. List product variants

**`GET /api/v1/products/{id}/variants`**

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/products/6c5078d3-f8e3-4c88-9afe-48b5423c664f/variants' \
  -H 'accept: application/json'
```

#### Response

```json
[
  {
    "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
    "productId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
    "sku": "SKU-12345",
    "name": "Example",
    "price": "250.00",
    "discountPrice": "250.00",
    "discountPercentage": 250.0,
    "barcode": "SAVE10",
    "weight": 250.0,
    "dimensions": "10x20x30 cm",
    "imageUrl": "string",
    "status": "ACTIVE",
    "isDefault": true,
    "sortOrder": 1,
    "stockQuantity": 1,
    "inStock": true,
    "options": [
      {
        "optionId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
        "optionName": "string",
        "valueId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
        "value": "10.00"
      }
    ]
  }
]
```

---

### 2. List product options

**`GET /api/v1/products/{id}/options`**

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/products/6c5078d3-f8e3-4c88-9afe-48b5423c664f/options' \
  -H 'accept: application/json'
```

#### Response

```json
[
  {
    "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
    "productId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
    "name": "Example",
    "sortOrder": 1,
    "values": [
      {
        "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
        "optionId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
        "value": "10.00",
        "sortOrder": 1
      }
    ]
  }
]
```

---
### 3. Create product variant

**`POST /api/v1/seller/products/{id}/variants`**

```bash
curl -X 'POST' \
  'http://localhost:8080/api/v1/seller/products/6c5078d3-f8e3-4c88-9afe-48b5423c664f/variants' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <seller_token>' \
  -H 'Content-Type: application/json' \
  -d '{
  "sku": "SKU-12345",
  "name": "Example",
  "price": "250.00",
  "discountPrice": "250.00",
  "barcode": "SAVE10",
  "weight": "string",
  "dimensions": "10x20x30 cm",
  "imageUrl": "string",
  "status": "ACTIVE",
  "isDefault": true,
  "sortOrder": 1
}'
```

#### Response

```json
{
  "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "productId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "sku": "SKU-12345",
  "name": "Example",
  "price": "250.00",
  "discountPrice": "250.00",
  "discountPercentage": 250.0,
  "barcode": "SAVE10",
  "weight": 250.0,
  "dimensions": "10x20x30 cm",
  "imageUrl": "string",
  "status": "ACTIVE",
  "isDefault": true,
  "sortOrder": 1,
  "stockQuantity": 1,
  "inStock": true,
  "options": [
    {
      "optionId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "optionName": "string",
      "valueId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "value": "10.00"
    }
  ]
}
```

---

### 4. Update product variant

**`PUT /api/v1/seller/products/{id}/variants/{variantId}`**

```bash
curl -X 'PUT' \
  'http://localhost:8080/api/v1/seller/products/6c5078d3-f8e3-4c88-9afe-48b5423c664f/variants/6c5078d3-f8e3-4c88-9afe-48b5423c664f' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <seller_token>' \
  -H 'Content-Type: application/json' \
  -d '{
  "sku": "SKU-12345",
  "name": "Example",
  "price": "250.00",
  "discountPrice": "250.00",
  "barcode": "SAVE10",
  "weight": "string",
  "dimensions": "10x20x30 cm",
  "imageUrl": "string",
  "status": "ACTIVE",
  "isDefault": true,
  "sortOrder": 1
}'
```

#### Response

```json
{
  "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "productId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "sku": "SKU-12345",
  "name": "Example",
  "price": "250.00",
  "discountPrice": "250.00",
  "discountPercentage": 250.0,
  "barcode": "SAVE10",
  "weight": 250.0,
  "dimensions": "10x20x30 cm",
  "imageUrl": "string",
  "status": "ACTIVE",
  "isDefault": true,
  "sortOrder": 1,
  "stockQuantity": 1,
  "inStock": true,
  "options": [
    {
      "optionId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "optionName": "string",
      "valueId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "value": "10.00"
    }
  ]
}
```

---

### 5. Delete product variant

**`DELETE /api/v1/seller/products/{id}/variants/{variantId}`**

```bash
curl -X 'DELETE' \
  'http://localhost:8080/api/v1/seller/products/6c5078d3-f8e3-4c88-9afe-48b5423c664f/variants/6c5078d3-f8e3-4c88-9afe-48b5423c664f' \
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

### 6. Attach option values to a variant

**`PUT /api/v1/seller/products/{id}/variants/{variantId}/options`**

```bash
curl -X 'PUT' \
  'http://localhost:8080/api/v1/seller/products/6c5078d3-f8e3-4c88-9afe-48b5423c664f/variants/6c5078d3-f8e3-4c88-9afe-48b5423c664f/options' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <seller_token>' \
  -H 'Content-Type: application/json' \
  -d '{
  "valueIds": [
    "string"
  ]
}'
```

#### Response

```json
{
  "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "productId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "sku": "SKU-12345",
  "name": "Example",
  "price": "250.00",
  "discountPrice": "250.00",
  "discountPercentage": 250.0,
  "barcode": "SAVE10",
  "weight": 250.0,
  "dimensions": "10x20x30 cm",
  "imageUrl": "string",
  "status": "ACTIVE",
  "isDefault": true,
  "sortOrder": 1,
  "stockQuantity": 1,
  "inStock": true,
  "options": [
    {
      "optionId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "optionName": "string",
      "valueId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "value": "10.00"
    }
  ]
}
```

---

### 7. Create option dimension

**`POST /api/v1/seller/products/{id}/options`**

```bash
curl -X 'POST' \
  'http://localhost:8080/api/v1/seller/products/6c5078d3-f8e3-4c88-9afe-48b5423c664f/options' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <seller_token>' \
  -H 'Content-Type: application/json' \
  -d '{
  "name": "Example",
  "sortOrder": 1
}'
```

#### Response

```json
{
  "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "productId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "name": "Example",
  "sortOrder": 1,
  "values": [
    {
      "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "optionId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "value": "10.00",
      "sortOrder": 1
    }
  ]
}
```

---

### 8. Delete option dimension

**`DELETE /api/v1/seller/products/{id}/options/{optionId}`**

```bash
curl -X 'DELETE' \
  'http://localhost:8080/api/v1/seller/products/6c5078d3-f8e3-4c88-9afe-48b5423c664f/options/6c5078d3-f8e3-4c88-9afe-48b5423c664f' \
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

### 9. Add option value

**`POST /api/v1/seller/products/{id}/options/{optionId}/values`**

```bash
curl -X 'POST' \
  'http://localhost:8080/api/v1/seller/products/6c5078d3-f8e3-4c88-9afe-48b5423c664f/options/6c5078d3-f8e3-4c88-9afe-48b5423c664f/values' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <seller_token>' \
  -H 'Content-Type: application/json' \
  -d '{
  "value": "10.00",
  "sortOrder": 1
}'
```

#### Response

```json
{
  "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "optionId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "value": "10.00",
  "sortOrder": 1
}
```

---

### 10. Delete option value

**`DELETE /api/v1/seller/products/{id}/options/{optionId}/values/{valueId}`**

```bash
curl -X 'DELETE' \
  'http://localhost:8080/api/v1/seller/products/6c5078d3-f8e3-4c88-9afe-48b5423c664f/options/6c5078d3-f8e3-4c88-9afe-48b5423c664f/values/6c5078d3-f8e3-4c88-9afe-48b5423c664f' \
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
