# Cart API

This documentation provides comprehensive details for the Cart API endpoints. The API supports adding products to cart, retrieving cart items, updating quantities, and removing items from the shopping cart. All cart operations are user-specific and require authentication to ensure cart data privacy and security.

**Base URL:** `http://localhost:8080`

## Authentication

All Cart endpoints require Bearer token authentication. Include the access token in the Authorization header:

```
Authorization: Bearer <your_access_token>
```

### Cart Management Endpoints

| Method | Endpoint | Description | Authentication Required |
|--------|----------|-------------|------------------------|
| `POST` | `/cart` | Add product to cart | Yes |
| `GET` | `/cart` | Retrieve cart items | Yes |
| `GET` | `/cart/summary` | Retrieve cart summary with totals | Yes |
| `PUT` | `/cart/update` | Update product quantity in cart | Yes |
| `DELETE` | `/cart/remove` | Remove specific product from cart | Yes |
| `DELETE` | `/cart/all` | Clear entire cart | Yes |

---

## Endpoint Details

### 1. Add Product to Cart

**`POST /cart`**

Add a product to the user's shopping cart with specified quantity. If the product already exists in the cart, this will add to the existing quantity.

#### Query Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `productId` | string | Yes | UUID of the product to add to cart |
| `quantity` | number | Yes | Quantity of the product to add (must be positive) |

#### Headers

| Header | Value | Required |
|--------|-------|----------|
| `Authorization` | `Bearer <access_token>` | Yes |

#### Example Request

```bash
curl -X 'POST' \
  'http://localhost:8080/cart?productId=5b24d429-c981-47c8-9318-f4d61dd2c1a4&quantity=1' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9...' \
  -d ''
```

#### Example Response

```json
{
  "productId": "5b24d429-c981-47c8-9318-f4d61dd2c1a4",
  "quantity": 1
}
```

#### Response Fields

| Field | Type | Description |
|-------|------|-------------|
| `productId` | string | UUID of the product added to cart |
| `quantity` | number | Total quantity of this product now in cart |

---

### 2. Get Cart Items

**`GET /cart`**

Retrieve all items in the user's shopping cart with detailed product information and pagination support.

#### Query Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `limit` | number | No | Maximum number of cart items to return (default: 10) |

#### Headers

| Header | Value | Required |
|--------|-------|----------|
| `Authorization` | `Bearer <access_token>` | Yes |

#### Example Request

```bash
curl -X 'GET' \
  'http://localhost:8080/cart?limit=10' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9...'
```

#### Example Response

```json
[
    {
      "productId": "71b26dd9-b4b5-4f87-a84d-c8daa506018a",
      "quantity": 3,
      "product": {
        "id": "71b26dd9-b4b5-4f87-a84d-c8daa506018a",
        "categoryId": "58f5c085-d04a-47de-beab-1d476b6ce432",
        "productName": "Smartch watch",
        "productCode": "string",
        "productQuantity": 5,
        "productDetail": "Xiaomi Smart Watch",
        "price": 10,
        "discountPrice": 0,
        "status": 0,
        "videoLink": "string",
        "mainSlider": "string",
        "hotDeal": "string",
        "bestRated": "string",
        "midSlider": "string",
        "hotNew": "string",
        "trend": "string",
        "buyOneGetOne": "string",
        "imageOne": "string",
        "imageTwo": "string"
      }
    }
  ]
}
```

#### Response Fields

| Field | Type | Description |
|-------|------|-------------|
| `data` | array | Array of cart item objects |
| `data[].productId` | string | UUID of the product in cart |
| `data[].quantity` | number | Quantity of this product in cart |
| `data[].product` | object | Complete product information object |
| `data[].product.id` | string | Product UUID |
| `data[].product.productName` | string | Name of the product |
| `data[].product.price` | number | Regular price of the product |
| `data[].product.discountPrice` | number | Discounted price (if applicable) |
| `data[].product.productDetail` | string | Detailed description of the product |

---

### 3. Update Cart Item Quantity

**`PUT /cart`**

Update the quantity of a specific product in the user's cart. This operation sets the absolute quantity rather than adding to existing quantity.

#### Query Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `productId` | string | Yes | UUID of the product to update |
| `quantity` | number | Yes | New quantity for the product (must be positive) |

#### Headers

| Header | Value | Required |
|--------|-------|----------|
| `Authorization` | `Bearer <access_token>` | Yes |

#### Example Request

```bash
curl -X 'PUT' \
  'http://localhost:8080/cart?productId=5b24d429-c981-47c8-9318-f4d61dd2c1a4&quantity=1' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9...'
```

#### Example Response

```json
{
    "productId": "5b24d429-c981-47c8-9318-f4d61dd2c1a4",
    "quantity": 2,
    "product": {
      "id": "5b24d429-c981-47c8-9318-f4d61dd2c1a4",
      "categoryId": "58f5c085-d04a-47de-beab-1d476b6ce432",
      "productName": "Polo T Shirt",
      "productCode": "string",
      "productQuantity": 1,
      "productDetail": "Chinese polo T-shirt",
      "price": 100,
      "discountPrice": 0,
      "status": 0,
      "hotDeal": "string",
      "bestRated": "string",
      "buyOneGetOne": "string"
    }
  }
}
```

#### Response Fields

| Field | Type | Description |
|-------|------|-------------|
| `productId` | string | UUID of the updated product |
| `quantity` | number | Updated quantity in cart |
| `product` | object | Complete product information object |

---

### 4. Remove Product from Cart

**`DELETE /cart`**

Remove a specific product from the user's cart completely, regardless of quantity.

#### Query Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `productId` | string | Yes | UUID of the product to remove from cart |

#### Headers

| Header | Value | Required |
|--------|-------|----------|
| `Authorization` | `Bearer <access_token>` | Yes |

#### Example Request

```bash
curl -X 'DELETE' \
  'http://localhost:8080/cart?productId=71b26dd9-b4b5-4f87-a84d-c8daa506018a' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9...'
```

#### Example Response

```json
{
    "id": "71b26dd9-b4b5-4f87-a84d-c8daa506018a",
    "categoryId": "58f5c085-d04a-47de-beab-1d476b6ce432",
    "productName": "Smartch watch",
    "productCode": "string",
    "productQuantity": 5,
    "productDetail": "Xiaomi Smart Watch",
    "price": 10,
    "discountPrice": 0,
    "status": 0,
    "videoLink": "string",
    "mainSlider": "string",
    "hotDeal": "string",
    "bestRated": "string",
    "midSlider": "string",
    "hotNew": "string",
    "trend": "string",
    "buyOneGetOne": "string",
    "imageOne": "string",
    "imageTwo": "string"
  }
}
```

#### Response Fields

| Field | Type | Description |
|-------|------|-------------|
| `data` | object | Complete information of the removed product |

---

### 5. Clear Cart

**`DELETE /cart/all`**

Remove all items from the user's cart, effectively clearing the entire shopping cart.

#### Headers

| Header | Value | Required |
|--------|-------|----------|
| `Authorization` | `Bearer <access_token>` | Yes |

#### Example Request

```bash
curl -X 'DELETE' \
  'http://localhost:8080/cart/all' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9...'
```

#### Example Response

**Status: 200 OK**

```json
true
```

#### Response Fields

| Field | Type | Description |
|-------|------|-------------|
| `data` | boolean | Indicates successful cart clearance |

---

### 6. Get Cart Summary

**`GET /cart/summary`**

Retrieve a summary of the user's cart including all items with product details, subtotal, estimated tax, and item count. This endpoint provides enriched data for checkout pages.

#### Headers

| Header | Value | Required |
|--------|-------|----------|
| `Authorization` | `Bearer <access_token>` | Yes |

#### Example Request

```bash
curl -X 'GET' \
  'http://localhost:8080/cart/summary' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9...'
```

#### Example Response

**Status: 200 OK**

```json
{
  "items": [
    {
      "productId": "71b26dd9-b4b5-4f87-a84d-c8daa506018a",
      "productName": "Smart Watch",
      "price": 10.0,
      "quantity": 3,
      "image": "https://example.com/image1.jpg",
      "stockQuantity": 50,
      "shopId": "shop-uuid-1",
      "shopName": "Electronics Shop"
    }
  ],
  "subtotal": 30.0,
  "estimatedTax": 3.0,
  "itemCount": 1
}
```

#### Response Fields

| Field | Type | Description |
|-------|------|-------------|
| `items` | array | Array of cart item objects with product details |
| `items[].productId` | string | UUID of the product |
| `items[].productName` | string | Name of the product |
| `items[].price` | number | Effective price (discount price if available, otherwise regular price) |
| `items[].quantity` | number | Quantity of this product in cart |
| `items[].image` | string | Primary product image URL |
| `items[].stockQuantity` | number | Current effective stock quantity |
| `items[].shopId` | string | Shop UUID (nullable) |
| `items[].shopName` | string | Shop name (nullable) |
| `subtotal` | number | Total price of all items before tax |
| `estimatedTax` | number | Estimated tax amount (10% of subtotal) |
| `itemCount` | number | Number of unique products in cart |

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
