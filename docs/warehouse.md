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
