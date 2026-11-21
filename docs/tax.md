# Tax Engine

Tax zones, classes, rules and exemptions resolved per order line.

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
| <span class="method-badge get">GET</span> | `/api/v1/admin/tax-rules` | List rules |
| <span class="method-badge post">POST</span> | `/api/v1/admin/tax-rules` | Create or update a rule |
| <span class="method-badge put">PUT</span> | `/api/v1/admin/tax-rules` | Upsert a rule |
| <span class="method-badge delete">DELETE</span> | `/api/v1/admin/tax-rules/{id}` | Delete a rule |
| <span class="method-badge get">GET</span> | `/api/v1/admin/tax-classes` | List tax classes |
| <span class="method-badge post">POST</span> | `/api/v1/admin/tax-classes` | Create a tax class |
| <span class="method-badge put">PUT</span> | `/api/v1/admin/tax-classes/{id}` | Update a tax class |
| <span class="method-badge delete">DELETE</span> | `/api/v1/admin/tax-classes/{id}` | Delete a tax class |
| <span class="method-badge get">GET</span> | `/api/v1/admin/tax-zones` | List zones |
| <span class="method-badge post">POST</span> | `/api/v1/admin/tax-zones` | Create a zone |
| <span class="method-badge put">PUT</span> | `/api/v1/admin/tax-zones/{id}` | Update a zone |
| <span class="method-badge delete">DELETE</span> | `/api/v1/admin/tax-zones/{id}` | Delete a zone |
| <span class="method-badge get">GET</span> | `/api/v1/admin/tax-exemptions` | List exemptions |
| <span class="method-badge post">POST</span> | `/api/v1/admin/tax-exemptions` | Create an exemption |
| <span class="method-badge delete">DELETE</span> | `/api/v1/admin/tax-exemptions/{id}` | Delete an exemption |
| <span class="method-badge put">PUT</span> | `/api/v1/admin/products/{id}/tax-class` | Assign a product tax class |

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
## Endpoint Details

### 1. List tax rules

<div class="endpoint-header">
  <span class="method-badge get">GET</span>
  <span class="endpoint-path">/api/v1/admin/tax-rules</span>
</div>

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/admin/tax-rules' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <admin_token>'
```

#### Response

```json
[
  {
    "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
    "country": "US",
    "categoryId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
    "zoneId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
    "taxClassId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
    "rate": "250.00",
    "isInclusive": true,
    "priority": 1,
    "isActive": true
  }
]
```

---

### 2. Create or update tax rule

<div class="endpoint-header">
  <span class="method-badge post">POST</span>
  <span class="endpoint-path">/api/v1/admin/tax-rules</span>
</div>

```bash
curl -X 'POST' \
  'http://localhost:8080/api/v1/admin/tax-rules' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <admin_token>' \
  -H 'Content-Type: application/json' \
  -d '{
  "country": "US",
  "categoryId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "zoneId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "taxClassId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "rate": "250.00",
  "isInclusive": true,
  "priority": 1,
  "isActive": true
}'
```

#### Response

```json
{
  "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "country": "US",
  "categoryId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "zoneId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "taxClassId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "rate": "250.00",
  "isInclusive": true,
  "priority": 1,
  "isActive": true
}
```

---

### 3. Upsert tax rule

<div class="endpoint-header">
  <span class="method-badge put">PUT</span>
  <span class="endpoint-path">/api/v1/admin/tax-rules</span>
</div>

```bash
curl -X 'PUT' \
  'http://localhost:8080/api/v1/admin/tax-rules' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <admin_token>' \
  -H 'Content-Type: application/json' \
  -d '{
  "country": "US",
  "categoryId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "zoneId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "taxClassId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "rate": "250.00",
  "isInclusive": true,
  "priority": 1,
  "isActive": true
}'
```

#### Response

```json
{
  "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "country": "US",
  "categoryId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "zoneId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "taxClassId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "rate": "250.00",
  "isInclusive": true,
  "priority": 1,
  "isActive": true
}
```

---

### 4. Delete tax rule

<div class="endpoint-header">
  <span class="method-badge delete">DELETE</span>
  <span class="endpoint-path">/api/v1/admin/tax-rules/{id}</span>
</div>

```bash
curl -X 'DELETE' \
  'http://localhost:8080/api/v1/admin/tax-rules/6c5078d3-f8e3-4c88-9afe-48b5423c664f' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <admin_token>'
```

#### Response

```json
{
  "message": "Operation completed"
}
```

---

### 5. List tax classes

<div class="endpoint-header">
  <span class="method-badge get">GET</span>
  <span class="endpoint-path">/api/v1/admin/tax-classes</span>
</div>

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/admin/tax-classes' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <admin_token>'
```

#### Response

```json
[
  {
    "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
    "name": "Example",
    "code": "SAVE10",
    "rate": "250.00",
    "isInclusive": true,
    "isActive": true
  }
]
```

---

### 6. Create a tax class

<div class="endpoint-header">
  <span class="method-badge post">POST</span>
  <span class="endpoint-path">/api/v1/admin/tax-classes</span>
</div>

```bash
curl -X 'POST' \
  'http://localhost:8080/api/v1/admin/tax-classes' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <admin_token>' \
  -H 'Content-Type: application/json' \
  -d '{
  "name": "Example",
  "code": "SAVE10",
  "rate": "250.00",
  "isInclusive": true,
  "isActive": true
}'
```

#### Response

```json
{
  "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "name": "Example",
  "code": "SAVE10",
  "rate": "250.00",
  "isInclusive": true,
  "isActive": true
}
```

---

### 7. Update a tax class

<div class="endpoint-header">
  <span class="method-badge put">PUT</span>
  <span class="endpoint-path">/api/v1/admin/tax-classes/{id}</span>
</div>

```bash
curl -X 'PUT' \
  'http://localhost:8080/api/v1/admin/tax-classes/6c5078d3-f8e3-4c88-9afe-48b5423c664f' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <admin_token>' \
  -H 'Content-Type: application/json' \
  -d '{
  "name": "Example",
  "code": "SAVE10",
  "rate": "250.00",
  "isInclusive": true,
  "isActive": true
}'
```

#### Response

```json
{
  "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "name": "Example",
  "code": "SAVE10",
  "rate": "250.00",
  "isInclusive": true,
  "isActive": true
}
```

---

### 8. Delete a tax class

<div class="endpoint-header">
  <span class="method-badge delete">DELETE</span>
  <span class="endpoint-path">/api/v1/admin/tax-classes/{id}</span>
</div>

```bash
curl -X 'DELETE' \
  'http://localhost:8080/api/v1/admin/tax-classes/6c5078d3-f8e3-4c88-9afe-48b5423c664f' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <admin_token>'
```

#### Response

```json
{
  "message": "Operation completed"
}
```

---

### 9. List tax zones

<div class="endpoint-header">
  <span class="method-badge get">GET</span>
  <span class="endpoint-path">/api/v1/admin/tax-zones</span>
</div>

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/admin/tax-zones' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <admin_token>'
```

#### Response

```json
[
  {
    "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
    "name": "Example",
    "country": "US",
    "region": "string",
    "isNexus": true,
    "priority": 1,
    "isActive": true
  }
]
```

---

### 10. Create a tax zone

<div class="endpoint-header">
  <span class="method-badge post">POST</span>
  <span class="endpoint-path">/api/v1/admin/tax-zones</span>
</div>

```bash
curl -X 'POST' \
  'http://localhost:8080/api/v1/admin/tax-zones' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <admin_token>' \
  -H 'Content-Type: application/json' \
  -d '{
  "name": "Example",
  "country": "US",
  "region": "string",
  "isNexus": true,
  "priority": 1,
  "isActive": true
}'
```

#### Response

```json
{
  "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "name": "Example",
  "country": "US",
  "region": "string",
  "isNexus": true,
  "priority": 1,
  "isActive": true
}
```

---

### 11. Update a tax zone

<div class="endpoint-header">
  <span class="method-badge put">PUT</span>
  <span class="endpoint-path">/api/v1/admin/tax-zones/{id}</span>
</div>

```bash
curl -X 'PUT' \
  'http://localhost:8080/api/v1/admin/tax-zones/6c5078d3-f8e3-4c88-9afe-48b5423c664f' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <admin_token>' \
  -H 'Content-Type: application/json' \
  -d '{
  "name": "Example",
  "country": "US",
  "region": "string",
  "isNexus": true,
  "priority": 1,
  "isActive": true
}'
```

#### Response

```json
{
  "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "name": "Example",
  "country": "US",
  "region": "string",
  "isNexus": true,
  "priority": 1,
  "isActive": true
}
```

---

### 12. Delete a tax zone

<div class="endpoint-header">
  <span class="method-badge delete">DELETE</span>
  <span class="endpoint-path">/api/v1/admin/tax-zones/{id}</span>
</div>

```bash
curl -X 'DELETE' \
  'http://localhost:8080/api/v1/admin/tax-zones/6c5078d3-f8e3-4c88-9afe-48b5423c664f' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <admin_token>'
```

#### Response

```json
{
  "message": "Operation completed"
}
```

---

### 13. List tax exemptions

<div class="endpoint-header">
  <span class="method-badge get">GET</span>
  <span class="endpoint-path">/api/v1/admin/tax-exemptions</span>
</div>

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/admin/tax-exemptions' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <admin_token>'
```

#### Response

```json
[
  {
    "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
    "userId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
    "businessTaxId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
    "country": "US",
    "reason": "Policy violation",
    "certificateUrl": "string",
    "isActive": true,
    "expiresAt": "2024-05-06T12:00:00",
    "createdAt": "2024-05-06T12:00:00"
  }
]
```

---

### 14. Create a tax exemption

<div class="endpoint-header">
  <span class="method-badge post">POST</span>
  <span class="endpoint-path">/api/v1/admin/tax-exemptions</span>
</div>

```bash
curl -X 'POST' \
  'http://localhost:8080/api/v1/admin/tax-exemptions' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <admin_token>' \
  -H 'Content-Type: application/json' \
  -d '{
  "userId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "businessTaxId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "country": "US",
  "reason": "Policy violation",
  "certificateUrl": "string",
  "expiresAt": "2024-05-06T12:00:00",
  "isActive": true
}'
```

#### Response

```json
{
  "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "userId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "businessTaxId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "country": "US",
  "reason": "Policy violation",
  "certificateUrl": "string",
  "isActive": true,
  "expiresAt": "2024-05-06T12:00:00",
  "createdAt": "2024-05-06T12:00:00"
}
```

---

### 15. Delete a tax exemption

<div class="endpoint-header">
  <span class="method-badge delete">DELETE</span>
  <span class="endpoint-path">/api/v1/admin/tax-exemptions/{id}</span>
</div>

```bash
curl -X 'DELETE' \
  'http://localhost:8080/api/v1/admin/tax-exemptions/6c5078d3-f8e3-4c88-9afe-48b5423c664f' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <admin_token>'
```

#### Response

```json
{
  "message": "Operation completed"
}
```

---

### 16. Assign a product tax class

<div class="endpoint-header">
  <span class="method-badge put">PUT</span>
  <span class="endpoint-path">/api/v1/admin/products/{id}/tax-class</span>
</div>

```bash
curl -X 'PUT' \
  'http://localhost:8080/api/v1/admin/products/6c5078d3-f8e3-4c88-9afe-48b5423c664f/tax-class' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <admin_token>' \
  -H 'Content-Type: application/json' \
  -d '{
  "taxClassId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f"
}'
```

#### Response

```json
{
  "message": "Operation completed"
}
```

---
