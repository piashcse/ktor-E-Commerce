# Localization & Localized Checkout

Locale negotiation, translated API messages, localized product content, and locale/currency-aware
checkout totals.

## Supported locales

| Code | Language | Default currency |
|---|---|---|
| `en` | English | USD |
| `es` | Español | EUR |
| `fr` | Français | EUR |
| `bn` | বাংলা | BDT |
| `hi` | हिन्दी | INR |

## Locale negotiation

Every request can select a locale with, in priority order:

1. `?lang=es`
2. `X-Locale: es`
3. `Accept-Language: es-MX,es;q=0.9,en;q=0.8`

Regional tags fall back to the base language. Unsupported locales fall back to `en`. Responses
carry `Content-Language`.

## Message catalogs

```bash
curl 'http://localhost:8080/api/v1/i18n/messages?locale=es'
```

Returns key → translated value for the catalog. Unknown messages fall back to English. Error
responses (`message`/`title`) are translated automatically when the error text is in the catalog.

```bash
curl 'http://localhost:8080/api/v1/i18n/locales'
```

## Localized checkout

`CheckoutRequest` accepts `locale` (defaults to `en`):

```json
{
  "shippingAddressId": "address-uuid",
  "shippingMethodId": "method-uuid",
  "currency": "EUR",
  "locale": "fr",
  "paymentMethod": "CREDIT_CARD"
}
```

- The locale is stored on the order and returned as `order.locale`.
- `OrderResponse.formattedTotal` uses the order currency + locale.
- Checkout summary returns `locale` plus `formattedSubTotal`, `formattedShipping`,
  `formattedTax`, `formattedDiscount` and `formattedTotal`.
- Currency conversion still comes from FX rates; the locale only changes presentation and
  translated content.

## Localized product content

Product detail, product lists, search results and recommendations use the request locale.
Fallback is the default product row.

Admin APIs:

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/v1/admin/products/{id}/translations` | List translations |
| `PUT` | `/api/v1/admin/products/{id}/translations/{locale}` | Create/update |
| `DELETE` | `/api/v1/admin/products/{id}/translations/{locale}` | Delete |
| `GET` | `/api/v1/i18n/locales` | List supported locales |
| `GET` | `/api/v1/i18n/messages` | Get translated API message catalog |

```json
{
  "name": "Chaise en bois",
  "description": "Chaise durable en bois massif."
}
```

Only supported locale codes are accepted. Translations are only applied when the code matches the
request locale; missing translations fall back to the product's default name/description.
## Endpoint Details

### 1. List a product's translations

**`GET /api/v1/admin/products/{id}/translations`**

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/admin/products/6c5078d3-f8e3-4c88-9afe-48b5423c664f/translations' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <admin_token>'
```

#### Response

```json
[
  {
    "productId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
    "locale": "en",
    "name": "Example",
    "description": "Looks good"
  }
]
```

---

### 2. Create or update a product translation

**`PUT /api/v1/admin/products/{id}/translations/{locale}`**

```bash
curl -X 'PUT' \
  'http://localhost:8080/api/v1/admin/products/6c5078d3-f8e3-4c88-9afe-48b5423c664f/translations/en' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <admin_token>' \
  -H 'Content-Type: application/json' \
  -d '{
  "name": "Example",
  "description": "Looks good"
}'
```

#### Response

```json
{
  "productId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "locale": "en",
  "name": "Example",
  "description": "Looks good"
}
```

---

### 3. Delete a product translation

**`DELETE /api/v1/admin/products/{id}/translations/{locale}`**

```bash
curl -X 'DELETE' \
  'http://localhost:8080/api/v1/admin/products/6c5078d3-f8e3-4c88-9afe-48b5423c664f/translations/en' \
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

### 4. List supported locales

**`GET /api/v1/i18n/locales`**

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/i18n/locales' \
  -H 'accept: application/json'
```

#### Response

```json
[
  {
    "code": "SAVE10",
    "language": "string",
    "englishName": "string",
    "defaultCurrency": "USD"
  }
]
```

---

### 5. Get translated API message catalog

**`GET /api/v1/i18n/messages`**

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/i18n/messages' \
  -H 'accept: application/json'
```

#### Response

```json
{
  "locale": "en",
  "messages": {}
}
```

---
