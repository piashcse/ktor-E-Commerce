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
| `PUT` | `/cart` | Update product quantity in cart | Yes |
| `DELETE` | `/cart` | Remove specific product from cart | Yes |
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
  "isSuccess": true,
  "statusCode": {
    "value": 200,
    "description": "OK"
  },
  "data": {
    "productId": "5b24d429-c981-47c8-9318-f4d61dd2c1a4",
    "quantity": 1
  }
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
{
  "isSuccess": true,
  "statusCode": {
    "value": 200,
    "description": "OK"
  },
  "data": [
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
  "isSuccess": true,
  "statusCode": {
    "value": 200,
    "description": "OK"
  },
  "data": {
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
  "isSuccess": true,
  "statusCode": {
    "value": 200,
    "description": "OK"
  },
  "data": {
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

```json
{
  "isSuccess": true,
  "statusCode": {
    "value": 200,
    "description": "OK"
  },
  "data": true
}
```

#### Response Fields

| Field | Type | Description |
|-------|------|-------------|
| `data` | boolean | Indicates successful cart clearance |

---

## Response Format

All API responses follow a consistent format:

```json
{
  "isSuccess": boolean,
  "statusCode": {
    "value": number,
    "description": string
  },
  "data": any
}
```

### Response Fields

| Field | Type | Description |
|-------|------|-------------|
| `isSuccess` | boolean | Indicates if the operation was successful |
| `statusCode.value` | number | HTTP status code |
| `statusCode.description` | string | HTTP status description |
| `data` | any | Response data (varies by endpoint) |

---

## Error Handling

The API returns appropriate HTTP status codes and error messages:

| Status Code | Description |
|-------------|-------------|
| `200` | OK - Request successful |
| `400` | Bad Request - Invalid parameters or negative quantity |
| `401` | Unauthorized - Invalid or missing authentication |
| `403` | Forbidden - Insufficient privileges |
| `404` | Not Found - Product not found or not in cart |
| `409` | Conflict - Product out of stock or quantity exceeds available stock |
| `500` | Internal Server Error - Server error |
