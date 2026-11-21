# Shipping Method

Delivery methods, prices and delivery estimates.

**Admin URL:** `http://localhost:8080/api/v1/admin/shipping-methods` (Admin)

> [!NOTE]
> Public lookup for shipping methods has been consolidated into the **Checkout** module at `/checkout/shipping-method`. Management operations (Create/Update/Delete) are now consistently located under the `/admin/shipping-methods` prefix.

## Authentication

- `GET` requests are public and do not require authentication.
- `POST`, `PUT`, and `DELETE` requests require Admin authentication.

```
Authorization: Bearer <admin_access_token>
```

### Endpoints

| Method | Endpoint | Description | Authentication Required | Role |
|--------|----------|-------------|------------------------|------|
| <span class="method-badge get">GET</span> | `/api/v1/checkout/shipping-method` | Retrieve all available shipping methods | No | Any |
| <span class="method-badge post">POST</span> | `/api/v1/admin/shipping-methods` | Create a new shipping method | Yes | Admin |
| <span class="method-badge put">PUT</span> | `/api/v1/admin/shipping-methods/{id}` | Update an existing shipping method | Yes | Admin |
| <span class="method-badge delete">DELETE</span> | `/api/v1/admin/shipping-methods/{id}` | Delete a shipping method | Yes | Admin |
| <span class="method-badge get">GET</span> | `/api/v1/seller/shipping-methods` | Seller - List my shipping methods | Yes | Seller |
| <span class="method-badge post">POST</span> | `/api/v1/seller/shipping-methods` | Seller - Create a shipping method | Yes | Seller |
| <span class="method-badge delete">DELETE</span> | `/api/v1/seller/shipping-methods/{id}` | Seller - Delete a shipping method | Yes | Seller |
| <span class="method-badge put">PUT</span> | `/api/v1/seller/shipping-methods/{id}` | Seller - Update a shipping method | Yes | Seller |

---

## Endpoint Details

### 1. Get All Shipping Methods

<div class="endpoint-header">
  <span class="method-badge get">GET</span>
  <span class="endpoint-path">/api/v1/checkout/shipping-method</span>
</div>

```json
[
  {
    "id": "1",
    "name": "Standard Shipping",
    "type": "Economy",
    "price": 5.0,
    "deliveryTime": "3-5 business days"
  },
  {
    "id": "2",
    "name": "Express Shipping",
    "type": "Premium",
    "price": 15.0,
    "deliveryTime": "1-2 business days"
  }
]
```

---

### 2. Create Shipping Method (Admin)

<div class="endpoint-header">
  <span class="method-badge post">POST</span>
  <span class="endpoint-path">/api/v1/admin/shipping-methods</span>
</div>

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `name` | string | Yes | Name of the shipping method |
| `type` | string | No | Type of shipping (e.g., Economy, Premium) |
| `price` | double | Yes | Cost of shipping |
| `deliveryTime` | string | No | Estimated delivery time |

#### Request

```json
{
  "name": "Overnight Shipping",
  "type": "Urgent",
  "price": 25.0,
  "deliveryTime": "Next business day"
}
```

---

### 3. Update Shipping Method (Admin)

<div class="endpoint-header">
  <span class="method-badge put">PUT</span>
  <span class="endpoint-path">/api/v1/admin/shipping-methods/{id}</span>
</div>

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `id` | string | Yes | Unique identifier of the shipping method to update |

#### Request Body

Same as **Create Shipping Method**.

---

### 4. Delete Shipping Method (Admin)

<div class="endpoint-header">
  <span class="method-badge delete">DELETE</span>
  <span class="endpoint-path">/api/v1/admin/shipping-methods/{id}</span>
</div>

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `id` | string | Yes | Unique identifier of the shipping method to delete |
### 5. Seller - List my shipping methods

<div class="endpoint-header">
  <span class="method-badge get">GET</span>
  <span class="endpoint-path">/api/v1/seller/shipping-methods</span>
</div>

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/seller/shipping-methods?page=1&perPage=20' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <seller_token>'
```

#### Response

```json
{
  "data": [
    {
      "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "name": "Example",
      "type": "CUSTOMER",
      "price": "250.00",
      "deliveryTime": "2024-05-06T12:00:00",
      "shopId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f"
    }
  ],
  "metadata": {
    "totalCount": 1,
    "limit": 20,
    "offset": 0,
    "page": 1,
    "perPage": 20,
    "totalPages": 1,
    "hasNext": false,
    "hasPrev": false
  }
}
```

---

### 6. Seller - Create a shipping method

<div class="endpoint-header">
  <span class="method-badge post">POST</span>
  <span class="endpoint-path">/api/v1/seller/shipping-methods</span>
</div>

```bash
curl -X 'POST' \
  'http://localhost:8080/api/v1/seller/shipping-methods' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <seller_token>' \
  -H 'Content-Type: application/json' \
  -d '{
  "name": "Example",
  "type": "CUSTOMER",
  "price": "250.00",
  "deliveryTime": "2024-05-06T12:00:00"
}'
```

#### Response

```json
{
  "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "name": "Example",
  "type": "CUSTOMER",
  "price": "250.00",
  "deliveryTime": "2024-05-06T12:00:00",
  "shopId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f"
}
```

---

### 7. Seller - Delete a shipping method

<div class="endpoint-header">
  <span class="method-badge delete">DELETE</span>
  <span class="endpoint-path">/api/v1/seller/shipping-methods/{id}</span>
</div>

```bash
curl -X 'DELETE' \
  'http://localhost:8080/api/v1/seller/shipping-methods/6c5078d3-f8e3-4c88-9afe-48b5423c664f' \
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

### 8. Seller - Update a shipping method

<div class="endpoint-header">
  <span class="method-badge put">PUT</span>
  <span class="endpoint-path">/api/v1/seller/shipping-methods/{id}</span>
</div>

```bash
curl -X 'PUT' \
  'http://localhost:8080/api/v1/seller/shipping-methods/6c5078d3-f8e3-4c88-9afe-48b5423c664f' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <seller_token>' \
  -H 'Content-Type: application/json' \
  -d '{
  "name": "Example",
  "type": "CUSTOMER",
  "price": "250.00",
  "deliveryTime": "2024-05-06T12:00:00"
}'
```

#### Response

```json
{
  "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "name": "Example",
  "type": "CUSTOMER",
  "price": "250.00",
  "deliveryTime": "2024-05-06T12:00:00",
  "shopId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f"
}
```

---
