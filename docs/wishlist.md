# Wishlist API

This documentation provides comprehensive details for the Wishlist API endpoints. The API supports adding products to wishlist, retrieving user's wishlist items, and removing products from wishlist. Wishlist functionality is user-specific and requires authentication to access personal wishlist data.

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
| `GET` | `/wishlist` | Retrieve user's wishlist items | Yes |
| `DELETE` | `/wishlist` | Remove a product from wishlist | Yes |

---

## Endpoint Details

### 1. Add Product to Wishlist

**`POST /wishlist`**

Add a product to the authenticated user's wishlist.

#### Query Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `productId` | string | Yes | UUID of the product to add to wishlist |

#### Headers

| Header | Value | Required |
|--------|-------|----------|
| `Authorization` | `Bearer <access_token>` | Yes |

#### Example Request

```bash
curl -X 'POST' \
  'http://localhost:8080/wishlist?productId=5b24d429-c981-47c8-9318-f4d61dd2c1a4' \
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
    "product": {
      "id": "5b24d429-c981-47c8-9318-f4d61dd2c1a4",
      "categoryId": "58f5c085-d04a-47de-beab-1d476b6ce432",
      "name": "Polo T Shirt",
      "productCode": "string",
      "productQuantity": 1,
      "description": "Chinese polo T-shirt",
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
| `data.product` | object | Complete product object added to wishlist |
| `data.product.id` | string | Unique identifier of the product |
| `data.product.categoryId` | string | UUID of the product category |
| `data.product.name` | string | Name of the product |
| `data.product.productCode` | string | Product code/SKU |
| `data.product.productQuantity` | number | Available quantity |
| `data.product.description` | string | Product description |
| `data.product.price` | number | Regular price of the product |
| `data.product.discountPrice` | number | Discounted price (0 if no discount) |
| `data.product.status` | number | Product status code |
| `data.product.hotDeal` | string | Hot deal indicator |
| `data.product.bestRated` | string | Best rated indicator |
| `data.product.buyOneGetOne` | string | Buy one get one offer indicator |

---

### 2. Get Wishlist Items

**`GET /wishlist`**

Retrieve all products in the authenticated user's wishlist.

#### Headers

| Header | Value | Required |
|--------|-------|----------|
| `Authorization` | `Bearer <access_token>` | Yes |

#### Example Request

```bash
curl -X 'GET' \
  'http://localhost:8080/wishlist' \
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
      "id": "5b24d429-c981-47c8-9318-f4d61dd2c1a4",
      "categoryId": "58f5c085-d04a-47de-beab-1d476b6ce432",
      "name": "Polo T Shirt",
      "productCode": "string",
      "productQuantity": 1,
      "description": "Chinese polo T-shirt",
      "price": 100,
      "discountPrice": 0,
      "status": 0,
      "hotDeal": "string",
      "bestRated": "string",
      "buyOneGetOne": "string"
    }
  ]
}
```

#### Response Fields

| Field | Type | Description |
|-------|------|-------------|
| `data` | array | Array of product objects in the wishlist |
| `data[].id` | string | Unique identifier of the product |
| `data[].categoryId` | string | UUID of the product category |
| `data[].name` | string | Name of the product |
| `data[].productCode` | string | Product code/SKU |
| `data[].productQuantity` | number | Available quantity |
| `data[].description` | string | Product description |
| `data[].price` | number | Regular price of the product |
| `data[].discountPrice` | number | Discounted price (0 if no discount) |
| `data[].status` | number | Product status code |
| `data[].hotDeal` | string | Hot deal indicator |
| `data[].bestRated` | string | Best rated indicator |
| `data[].buyOneGetOne` | string | Buy one get one offer indicator |

---

### 3. Remove Product from Wishlist

**`DELETE /wishlist`**

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
  'http://localhost:8080/wishlist?productId=5b24d429-c981-47c8-9318-f4d61dd2c1a4' \
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
    "id": "5b24d429-c981-47c8-9318-f4d61dd2c1a4",
    "categoryId": "58f5c085-d04a-47de-beab-1d476b6ce432",
    "name": "Polo T Shirt",
    "productCode": "string",
    "productQuantity": 1,
    "description": "Chinese polo T-shirt",
    "price": 100,
    "discountPrice": 0,
    "status": 0,
    "hotDeal": "string",
    "bestRated": "string",
    "buyOneGetOne": "string"
  }
}
```

#### Response Fields

| Field | Type | Description |
|-------|------|-------------|
| `data` | object | Complete product object removed from wishlist |
| `data.id` | string | Unique identifier of the removed product |
| `data.categoryId` | string | UUID of the product category |
| `data.name` | string | Name of the product |
| `data.productCode` | string | Product code/SKU |
| `data.productQuantity` | number | Available quantity |
| `data.description` | string | Product description |
| `data.price` | number | Regular price of the product |
| `data.discountPrice` | number | Discounted price (0 if no discount) |
| `data.status` | number | Product status code |
| `data.hotDeal` | string | Hot deal indicator |
| `data.bestRated` | string | Best rated indicator |
| `data.buyOneGetOne` | string | Buy one get one offer indicator |

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
| `400` | Bad Request - Invalid product ID or missing required fields |
| `401` | Unauthorized - Invalid or missing authentication |
| `403` | Forbidden - Insufficient privileges |
| `404` | Not Found - Product not found or not in wishlist |
| `409` | Conflict - Product already exists in wishlist |
| `500` | Internal Server Error - Server error |