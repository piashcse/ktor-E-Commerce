# Tax Engine

Tax is resolved per order line using zones/nexus, product tax classes, and exemptions.
Rates are decimal fractions (`0.15` = 15%). Each line snapshots its rate, class and
inclusive/exclusive mode on `order_item`; the checkout summary returns a per-line breakdown.

## Resolution order

1. Customer/business exemption (active, matching country or global, not expired) → zero tax.
2. A matching `tax_zone` with `isNexus = false` → zero tax.
3. The most specific active rule for the destination country:
   - zone-scoped beats country-wide
   - class-scoped beats class-agnostic
   - category-scoped beats category-agnostic
   - then highest `priority`, then oldest rule.
4. The assigned product tax class default rate/mode.
5. Application default (`TAX_DEFAULT_RATE`, default 5%).

A rule is only eligible when its zone/class/category (when set) matches the line.

## Inclusive vs exclusive

- **Exclusive** (default): the line amount is net; tax is added to the order total.
- **Inclusive**: the line amount is gross; tax is carved out (`net = gross / (1 + rate)`).
  The order total does not add inclusive tax again.

## Admin endpoints

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/v1/admin/tax-rules` | List rules |
| `POST` | `/api/v1/admin/tax-rules` | Create or update a rule |
| `PUT` | `/api/v1/admin/tax-rules` | Upsert a rule |
| `DELETE` | `/api/v1/admin/tax-rules/{id}` | Delete a rule |
| `GET` | `/api/v1/admin/tax-classes` | List tax classes |
| `POST` | `/api/v1/admin/tax-classes` | Create a tax class |
| `PUT` | `/api/v1/admin/tax-classes/{id}` | Update a tax class |
| `DELETE` | `/api/v1/admin/tax-classes/{id}` | Delete a tax class |
| `GET` | `/api/v1/admin/tax-zones` | List zones |
| `POST` | `/api/v1/admin/tax-zones` | Create a zone |
| `PUT` | `/api/v1/admin/tax-zones/{id}` | Update a zone |
| `DELETE` | `/api/v1/admin/tax-zones/{id}` | Delete a zone |
| `GET` | `/api/v1/admin/tax-exemptions` | List exemptions |
| `POST` | `/api/v1/admin/tax-exemptions` | Create an exemption |
| `DELETE` | `/api/v1/admin/tax-exemptions/{id}` | Delete an exemption |
| `PUT` | `/api/v1/admin/products/{id}/tax-class` | Assign a product tax class |

### Tax rule body

```json
{
  "country": "BD",
  "categoryId": "category-uuid",
  "zoneId": "zone-uuid",
  "taxClassId": "class-uuid",
  "rate": 0.15,
  "isInclusive": true,
  "priority": 10,
  "isActive": true
}
```

### Tax class body

```json
{ "name": "Reduced", "code": "REDUCED", "rate": 0.10, "isInclusive": true, "isActive": true }
```

### Tax zone body

```json
{ "name": "Dhaka", "country": "BD", "region": "Dhaka", "isNexus": true, "priority": 10, "isActive": true }
```

### Exemption body

```json
{
  "userId": "user-uuid",
  "businessTaxId": "BIN-123",
  "country": "BD",
  "reason": "Reseller certificate",
  "certificateUrl": "https://example.com/cert.pdf",
  "expiresAt": "2027-01-01T00:00:00"
}
```

At least one of `userId` or `businessTaxId` is required. A null `country` applies to every
destination. Checkout accepts `businessTaxId` and stores it with `taxExempt` on the order.

## Per-line breakdown

`GET /api/v1/checkout/summary` (and order responses) expose:

- `order_item.taxRate`, `taxInclusive`, `taxClassId`, `taxAmount`
- `taxLines[]` with `productId`, `taxClassId`, `rate`, `inclusive`, `netAmount`,
  `taxAmount`, `grossAmount`

Tax is calculated on the post-discount item total and snapshotted on the order line.
