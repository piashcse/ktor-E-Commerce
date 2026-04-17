# Wishlist API

This documentation provides comprehensive details for the Wishlist API endpoints. The API supports adding products to wishlist, retrieving user's wishlist items, checking product status, and removing products from wishlist. Wishlist functionality is user-specific and requires authentication to access personal wishlist data.

**Base URL:** `http://localhost:8080`

## Authentication

All Wishlist endpoints require Bearer token authentication. Include the access token in the Authorization header:

```
Authorization: Bearer <your_access_token>
```

### Wishlist Endpoints

| Method | Endpoint | Description | Authentication Required |
|--------|----------|-------------|------------------------|
| `POST` | `/wishlist` | Add a product to wishlist | Yes |
| `GET` | `/wishlist` | Retrieve user's wishlist items (Paginated) | Yes |
| `GET` | `/wishlist/check` | Check if a product is in wishlist | Yes |
| `DELETE` | `/wishlist/remove` | Remove a product from wishlist | Yes |

---

## Endpoint Details

### 1. Add Product to Wishlist

**`POST /wishlist`**

Add a product to the authenticated user's wishlist.

#### Request Body

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `productId` | string | Yes | UUID of the product to add to wishlist |

#### Headers

| Header | Value | Required |
|--------|-------|----------|
| `Authorization` | `Bearer <access_token>` | Yes |
| `Content-Type` | `application/json` | Yes |

#### Example Request

```bash
curl -X 'POST' \
  'http://localhost:8080/wishlist' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9...' \
  -H 'Content-Type: application/json' \
  -d '{
  "productId": "5b24d429-c981-47c8-9318-f4d61dd2c1a4"
}'
```

#### Example Response

```json
{
    "product": {
      "id": "5b24d429-c981-47c8-9318-f4d61dd2c1a4",
      "categoryId": "58f5c085-d04a-47de-beab-1d476b6ce432",
      "name": "Polo T Shirt",
      // ... product details
    }
  }
}
```

---

### 2. Get Wishlist Items

**`GET /wishlist`**

Retrieve all products in the authenticated user's wishlist with pagination.

#### Query Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `limit` | number | No | Maximum number of items to return (default: 20) |
| `offset` | number | No | Number of items to skip for pagination (default: 0) |

#### Headers

| Header | Value | Required |
|--------|-------|----------|
| `Authorization` | `Bearer <access_token>` | Yes |

#### Example Request

```bash
curl -X 'GET' \
  'http://localhost:8080/wishlist?page=1&limit=10' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9...'
```

#### Example Response

```json
{
  "data": [
    {
      "id": "5b24d429-c981-47c8-9318-f4d61dd2c1a4",
      "name": "Polo T Shirt"
      // ... product details
    }
  ],
  "metadata": {
    "totalCount": 1,
    "limit": 10,
    "skip": 0
  }
}
```

---

### 3. Check Product in Wishlist

**`GET /wishlist/check`**

Check if a specific product exists in the user's wishlist.

#### Query Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `productId` | string | Yes | UUID of the product to check |

#### Example Request

```bash
curl -X 'GET' \
  'http://localhost:8080/wishlist/check?productId=5b24d429-c981-47c8-9318-f4d61dd2c1a4' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <token>'
```

#### Example Response

```json
true
```

---

### 4. Remove Product from Wishlist

**`DELETE /wishlist/remove`**

Remove a specific product from the authenticated user's wishlist.

#### Query Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `productId` | string | Yes | UUID of the product to remove from wishlist |

#### Headers

| Header | Value | Required |
|--------|-------|----------|
| `Authorization` | `Bearer <access_token>` | Yes |

#### Example Request

```bash
curl -X 'DELETE' \
  'http://localhost:8080/wishlist/remove?productId=5b24d429-c981-47c8-9318-f4d61dd2c1a4' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9...'
```

#### Example Response

```json
{
      "id": "5b24d429-c981-47c8-9318-f4d61dd2c1a4",
      "name": "Polo T Shirt",
      // ... product details
  }
}
```

---

## Error Handling

This API follows industry-standard error handling patterns (Stripe, GitHub, OpenAI):

### Success Responses
- **HTTP status code indicates success** (200, 201, 204)
- **Response body contains data directly** (no wrapper object)
- No `isSuccess` or `statusCode` fields needed

### Error Responses

**Standard Error (400/401/403/404/500):**
```json
{
  "message": "Error description"
}
```

**Validation Error (400):**
```json
{
  "message": "Validation failed",
  "errors": [
    {"field": "email", "message": "Invalid email format"},
    {"field": "password", "message": "Password must be at least 8 characters"}
  ]
}
```

### Common Error Codes

| Status Code | Description | Example Message |
|-------------|-------------|-----------------|
| `400` | Bad Request | `"Invalid email or password"` |
| `401` | Unauthorized | `"Authentication required"` |
| `403` | Forbidden | `"Insufficient permissions"` |
| `404` | Not Found | `"Product not found"` |
| `409` | Conflict | `"User already exists with this email"` |
| `500` | Internal Server Error | `"Internal server error"` |

All error messages are centralized and consistent across all endpoints.

---
