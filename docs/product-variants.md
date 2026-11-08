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
| `POST` | `/{productId}/variants` | Create a variant |
| `PUT` | `/{productId}/variants/{variantId}` | Update a variant |
| `DELETE` | `/{productId}/variants/{variantId}` | Delete a variant |
| `PUT` | `/{productId}/variants/{variantId}/options` | Replace attached option values |
| `POST` | `/{productId}/options` | Create an option dimension, e.g. `Size` |
| `DELETE` | `/{productId}/options/{optionId}` | Delete an option and its values |
| `POST` | `/{productId}/options/{optionId}/values` | Add a value, e.g. `M` |
| `DELETE` | `/{productId}/options/{optionId}/values/{valueId}` | Delete a value |

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
