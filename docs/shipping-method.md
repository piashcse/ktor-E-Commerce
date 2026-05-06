# Shipping Method API

This documentation provides details for the Shipping Method API endpoints. Shipping methods define delivery options, prices, and estimated delivery times.

**Base URL:** `http://localhost:8080/api/v1/checkout/shipping-method` (Public)
**Admin URL:** `http://localhost:8080/api/v1/admin/shipping-method` (Admin)

> [!NOTE]
> Public lookup for shipping methods has been consolidated into the **Checkout** module at `/checkout/shipping-method`. Management operations (Create/Update/Delete) are now consistently located under the `/admin/shipping-method` prefix.

## Authentication

- `GET` requests are public and do not require authentication.
- `POST`, `PUT`, and `DELETE` requests require Admin authentication.

```
Authorization: Bearer <admin_access_token>
```

### Endpoints

| Method | Endpoint | Description | Authentication Required | Role |
|--------|----------|-------------|------------------------|------|
| `GET` | `/` | Retrieve all available shipping methods | No | Any |
| `POST` | `/` | Create a new shipping method | Yes | Admin |
| `PUT` | `/{id}` | Update an existing shipping method | Yes | Admin |
| `DELETE` | `/{id}` | Delete a shipping method | Yes | Admin |

---

## Endpoint Details

### 1. Get All Shipping Methods

**`GET /`**

Retrieve all available shipping methods.

#### Example Response

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

**`POST /`**

Create a new shipping method.

#### Request Body

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `name` | string | Yes | Name of the shipping method |
| `type` | string | No | Type of shipping (e.g., Economy, Premium) |
| `price` | double | Yes | Cost of shipping |
| `deliveryTime` | string | No | Estimated delivery time |

#### Example Request

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

**`PUT /{id}`**

Update an existing shipping method.

#### Path Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `id` | string | Yes | Unique identifier of the shipping method to update |

#### Request Body

Same as **Create Shipping Method**.

---

### 4. Delete Shipping Method (Admin)

**`DELETE /{id}`**

Delete a shipping method by its ID.

#### Path Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `id` | string | Yes | Unique identifier of the shipping method to delete |
