# B2B Quotes, Purchase Orders & Net Terms

Wholesale accounts, negotiated quotes, purchase orders with credit limits and payment terms.

**Base URL:** `http://localhost:8080`

## Accounts

**`POST /api/v1/b2b/account`**

```json
{
  "companyName": "Acme Wholesale",
  "taxId": "TAX-123",
  "contactName": "Alice",
  "phone": "+123456789"
}
```

- New accounts start `PENDING`.
- Re-submitting updates company/contact details without changing approval state.
- `GET /api/v1/b2b/account` returns credit limit, terms and status.

Admin approval:

```json
{
  "status": "APPROVED",
  "creditLimit": 5000.00,
  "paymentTermsDays": 30
}
```

## Quotes

**`POST /api/v1/b2b/quotes`**

```json
{
  "items": [
    { "productId": "product-a", "quantity": 10 },
    { "productId": "product-b", "quantity": 5 }
  ],
  "notes": "Please quote wholesale pricing",
  "validDays": 14
}
```

- Requires an `APPROVED` B2B account.
- All items must belong to one seller/shop.
- Initial prices use the product price/discount price.

Seller response:

**`PUT /api/v1/seller/b2b/quotes/{id}`**

```json
{
  "status": "SENT",
  "items": [
    { "productId": "product-a", "quantity": 10, "unitPrice": 8.50 },
    { "productId": "product-b", "quantity": 5, "unitPrice": 12.00 }
  ],
  "validUntil": "2026-06-15T00:00:00"
}
```

`status` may be `SENT` or `REJECTED`. Only the quote's seller can respond.

Other endpoints:

- `GET /api/v1/b2b/quotes`
- `GET /api/v1/b2b/quotes/{id}`
- `GET /api/v1/seller/b2b/quotes`

## Purchase orders and net terms

**`POST /api/v1/b2b/quotes/{id}/accept`**

- Only a `SENT`, unexpired quote can be accepted.
- Available credit = `creditLimit - open purchase orders`.
- Creates an `OPEN` purchase order with a unique PO number.
- Sets `dueDate = now + paymentTermsDays`.
- Marks the quote `ACCEPTED`.

Buyer:

- `GET /api/v1/b2b/purchase-orders`
- `GET /api/v1/b2b/purchase-orders/{id}`

Seller:

- `GET /api/v1/seller/b2b/purchase-orders`
- `POST /api/v1/seller/b2b/purchase-orders/{id}/fulfill` — creates the buyer order, decrements
  stock, links the PO to the order and sets `FULFILLED`

Admin:

- `GET /api/v1/admin/b2b/accounts?status=APPROVED`
- `PUT /api/v1/admin/b2b/accounts/{userId}`
- `GET /api/v1/admin/b2b/purchase-orders`

PO statuses: `OPEN` → `FULFILLED`; `CANCELED` is reserved for future cancellation flows.
The PO response includes the generated `orderId` after fulfillment.

## Invoices and dunning

Fulfilling a PO creates a net-terms invoice (`PENDING`) with a unique invoice number and the PO
due date.

Buyer / seller / admin:

- `GET /api/v1/b2b/invoices`
- `GET /api/v1/seller/b2b/invoices`
- `GET /api/v1/admin/b2b/invoices`

Admin actions:

- `POST /api/v1/admin/b2b/invoices/run-dunning` — marks due invoices `OVERDUE`, increments the
  reminder count and schedules the next reminder in 7 days. Returns `{ "reminded": N }`.
- `POST /api/v1/admin/b2b/invoices/{id}/pay?providerReference=wire-123` — records settlement,
  marks the invoice `PAID`, and marks the linked order payment `COMPLETED` (and `PAID` while it is
  still `PENDING`/`CONFIRMED`).

Invoice statuses: `PENDING` → `OVERDUE` → `PAID`; `VOID` is reserved for cancellation/credit-note
flows.

## Limits

- Quote items must be positive quantities.
- Unit prices must be positive.
- Credit limit cannot be negative; terms must be positive.
- The platform tracks net-terms invoices and reminders but does not auto-charge bank transfers;
  settlement remains an accounting/ERP integration.
