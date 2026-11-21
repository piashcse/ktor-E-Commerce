# Shipping Address

Saved shipping addresses for authenticated customers.

> [!NOTE]
> Shipping addresses are nested under the **Checkout** module. There is no standalone `/shipping-address` prefix — all endpoints below live under `/api/v1/checkout/shipping-address` (see `CheckoutRoutes.kt`).

## Authentication

All Shipping Address endpoints require Bearer token authentication. Include the access token in the Authorization header:

```
Authorization: Bearer <your_access_token>
```

### Endpoints

| Method | Endpoint | Description | Authentication Required |
|--------|----------|-------------|------------------------|
| <span class="method-badge get">GET</span> | `/api/v1/checkout/shipping-address` | Retrieve all shipping addresses for the authenticated user | Yes |
| <span class="method-badge post">POST</span> | `/api/v1/checkout/shipping-address` | Add a new shipping address | Yes |
| <span class="method-badge put">PUT</span> | `/api/v1/checkout/shipping-address/{id}` | Update an existing shipping address | Yes |
| <span class="method-badge delete">DELETE</span> | `/api/v1/checkout/shipping-address/{id}` | Delete a shipping address | Yes |

---

## Endpoint Details

### 1. Get All Shipping Addresses

<div class="endpoint-header">
  <span class="method-badge get">GET</span>
  <span class="endpoint-path">/api/v1/checkout/shipping-address</span>
</div>

```json
[
  {
    "id": "5489a8b4-7a16-4854-b157-396a8a731032",
    "userId": "a67fd0cc-3d92-4259-bbd4-1e0ba49dece4",
    "firstName": "John",
    "lastName": "Doe",
    "email": "john.doe@example.com",
    "phoneNumber": "+1234567890",
    "streetAddress": "123 Main St",
    "city": "New York",
    "state": "NY",
    "country": "USA",
    "zipCode": "10001",
    "isDefault": true
  }
]
```

---

### 2. Add Shipping Address

<div class="endpoint-header">
  <span class="method-badge post">POST</span>
  <span class="endpoint-path">/api/v1/checkout/shipping-address</span>
</div>

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `firstName` | string | Yes | First name of the recipient |
| `lastName` | string | Yes | Last name of the recipient |
| `email` | string | Yes | Email for shipping contact |
| `phoneNumber` | string | Yes | Phone number for shipping contact |
| `streetAddress` | string | Yes | Street address |
| `city` | string | Yes | City |
| `state` | string | No | State or province |
| `country` | string | Yes | Country |
| `zipCode` | string | Yes | Zip or postal code |
| `isDefault` | boolean | No | Whether this is the default address (default: false) |

#### Request

```json
{
  "firstName": "John",
  "lastName": "Doe",
  "email": "john.doe@example.com",
  "phoneNumber": "+1234567890",
  "streetAddress": "123 Main St",
  "city": "New York",
  "state": "NY",
  "country": "USA",
  "zipCode": "10001",
  "isDefault": true
}
```

---

### 3. Update Shipping Address

<div class="endpoint-header">
  <span class="method-badge put">PUT</span>
  <span class="endpoint-path">/api/v1/checkout/shipping-address/{id}</span>
</div>

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `id` | string | Yes | Unique identifier of the shipping address to update |

#### Request Body

Same as **Add Shipping Address**.

---

### 4. Delete Shipping Address

<div class="endpoint-header">
  <span class="method-badge delete">DELETE</span>
  <span class="endpoint-path">/api/v1/checkout/shipping-address/{id}</span>
</div>

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `id` | string | Yes | Unique identifier of the shipping address to delete |
