# Tax Rules

Tax is resolved per order line from country/category rules, with a configurable application default.

## Resolution order

1. Category-specific rule for the destination country.
2. Country default rule (`categoryId = null`).
3. Application default (`TAX_DEFAULT_RATE`, default 5%).

## Admin endpoints

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/v1/admin/tax-rules` | List rules |
| `POST` | `/api/v1/admin/tax-rules` | Create or update a rule |
| `PUT` | `/api/v1/admin/tax-rules` | Upsert a rule |
| `DELETE` | `/api/v1/admin/tax-rules/{id}` | Delete a rule |

Body:

```json
{
  "country": "BD",
  "categoryId": "category-uuid",
  "rate": 0.15,
  "isActive": true
}
```

`rate` is a decimal fraction (`0.15` = 15%) and must be between `0` and `1`.

Tax is calculated on the post-discount item total and snapshotted on `order_item.tax_amount`
and `order.tax_amount`.
