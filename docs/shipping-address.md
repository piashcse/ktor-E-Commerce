# Shipping Address API

This documentation provides details for the Shipping Address API endpoints. The API supports managing multiple saved shipping addresses for authenticated users.

**Base URL:** `http://localhost:8080/api/v1/checkout/shipping-address`

> [!NOTE]
> This module has been consolidated into the **Checkout** module. While the previous endpoints are maintained for backward compatibility, it is recommended to use the `/checkout/shipping-address` prefix for new integrations.

## Authentication

All Shipping Address endpoints require Bearer token authentication. Include the access token in the Authorization header:

```
Authorization: Bearer <your_access_token>
```

### Endpoints

| Method | Endpoint | Description | Authentication Required |
|--------|----------|-------------|------------------------|
| `GET` | `/` | Retrieve all shipping addresses for the authenticated user | Yes |
| `POST` | `/` | Add a new shipping address | Yes |
| `PUT` | `/{id}` | Update an existing shipping address | Yes |
| `DELETE` | `/{id}` | Delete a shipping address | Yes |

---

## Endpoint Details

### 1. Get All Shipping Addresses

**`GET /`**

Retrieve all shipping addresses associated with the authenticated user.

#### Example Response

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

**`POST /`**

Add a new shipping address for the authenticated user.

#### Request Body

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

#### Example Request

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

**`PUT /{id}`**

Update an existing shipping address.

#### Path Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `id` | string | Yes | Unique identifier of the shipping address to update |

#### Request Body

Same as **Add Shipping Address**.

---

### 4. Delete Shipping Address

**`DELETE /{id}`**

Delete a shipping address by its ID.

#### Path Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `id` | string | Yes | Unique identifier of the shipping address to delete |
