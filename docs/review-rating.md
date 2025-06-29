# Review Rating API

This documentation provides comprehensive details for the Review Rating API endpoints. The API supports creating, retrieving, updating, and deleting product reviews and ratings within the platform. Reviews are associated with specific products and users, enabling a complete feedback system for e-commerce platforms.

**Base URL:** `http://localhost:8080`

## Authentication

All Review Rating endpoints require Bearer token authentication. Include the access token in the Authorization header:

```
Authorization: Bearer <your_access_token>
```

### Review Rating Endpoints

| Method | Endpoint | Description | Authentication Required |
|--------|----------|-------------|------------------------|
| `GET` | `/review-rating` | Retrieve product reviews | Yes |
| `POST` | `/review-rating` | Create a new review | Yes |
| `PUT` | `/review-rating/{id}` | Update an existing review | Yes |
| `DELETE` | `/review-rating/{id}` | Delete a review | Yes |

---

## Endpoint Details

### 1. Get Reviews

**`GET /review-rating`**

Retrieve reviews for a specific product with optional pagination support.

#### Query Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `productId` | string | Yes | UUID of the product to get reviews for |
| `limit` | number | No | Maximum number of reviews to return (default: 10) |

#### Headers

| Header | Value | Required |
|--------|-------|----------|
| `Authorization` | `Bearer <access_token>` | Yes |

#### Example Request

```bash
curl -X 'GET' \
  'http://localhost:8080/review-rating?productId=cbd630f6-bf9f-48ad-ac51-f806807d99fd&limit=10' \
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
      "id": "70ac842b-7a81-4976-9564-d440880d1736",
      "userId": "a67fd0cc-3d92-4259-bbd4-1e0ba49dece4",
      "productId": "cbd630f6-bf9f-48ad-ac51-f806807d99fd",
      "reviewText": "Good product",
      "rating": 2
    }
  ]
}
```

#### Response Fields

| Field | Type | Description |
|-------|------|-------------|
| `data` | array | Array of review objects |
| `data[].id` | string | Unique identifier for the review |
| `data[].userId` | string | UUID of the user who created the review |
| `data[].productId` | string | UUID of the reviewed product |
| `data[].reviewText` | string | Text content of the review |
| `data[].rating` | number | Numerical rating (typically 1-5) |

---

### 2. Create Review

**`POST /review-rating`**

Create a new review and rating for a product. The review will be associated with the authenticated user and specified product.

#### Request Body

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `productId` | string | Yes | UUID of the product being reviewed |
| `rating` | number | Yes | Numerical rating for the product |
| `reviewText` | string | Yes | Text content of the review |

#### Headers

| Header | Value | Required |
|--------|-------|----------|
| `Authorization` | `Bearer <access_token>` | Yes |
| `Content-Type` | `application/json` | Yes |

#### Example Request

```bash
curl -X 'POST' \
  'http://localhost:8080/review-rating' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9...' \
  -H 'Content-Type: application/json' \
  -d '{
  "productId": "cbd630f6-bf9f-48ad-ac51-f806807d99fd",
  "rating": 2,
  "reviewText": "Good product"
}'
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
    "id": "70ac842b-7a81-4976-9564-d440880d1736",
    "userId": "a67fd0cc-3d92-4259-bbd4-1e0ba49dece4",
    "productId": "cbd630f6-bf9f-48ad-ac51-f806807d99fd",
    "reviewText": "Good product",
    "rating": 2
  }
}
```

#### Response Fields

| Field | Type | Description |
|-------|------|-------------|
| `id` | string | Unique identifier for the created review |
| `userId` | string | UUID of the user who created the review |
| `productId` | string | UUID of the reviewed product |
| `reviewText` | string | Text content of the review |
| `rating` | number | Numerical rating for the product |

---

### 3. Update Review

**`PUT /review-rating/{id}`**

Update an existing review by its ID. You can modify both the review text and rating.

#### Path Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `id` | string | Yes | Unique identifier of the review to update |

#### Query Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `review` | string | No | Updated review text content |
| `rating` | number | No | Updated numerical rating |

#### Headers

| Header | Value | Required |
|--------|-------|----------|
| `Authorization` | `Bearer <access_token>` | Yes |

#### Example Request

```bash
curl -X 'PUT' \
  'http://localhost:8080/review-rating/70ac842b-7a81-4976-9564-d440880d1736?review=Product%20review%20edited&rating=5' \
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
    "id": "70ac842b-7a81-4976-9564-d440880d1736",
    "userId": "a67fd0cc-3d92-4259-bbd4-1e0ba49dece4",
    "productId": "cbd630f6-bf9f-48ad-ac51-f806807d99fd",
    "reviewText": "Product review edited",
    "rating": 5
  }
}
```

#### Response Fields

| Field | Type | Description |
|-------|------|-------------|
| `id` | string | Unique identifier of the updated review |
| `userId` | string | UUID of the user who created the review |
| `productId` | string | UUID of the reviewed product |
| `reviewText` | string | Updated review text content |
| `rating` | number | Updated numerical rating |

---

### 4. Delete Review

**`DELETE /review-rating/{id}`**

Delete a review by its ID. This operation permanently removes the review from the system.

#### Path Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `id` | string | Yes | Unique identifier of the review to delete |

#### Headers

| Header | Value | Required |
|--------|-------|----------|
| `Authorization` | `Bearer <access_token>` | Yes |

#### Example Request

```bash
curl -X 'DELETE' \
  'http://localhost:8080/review-rating/70ac842b-7a81-4976-9564-d440880d1736' \
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
  "data": "70ac842b-7a81-4976-9564-d440880d1736"
}
```

#### Response Fields

| Field | Type | Description |
|-------|------|-------------|
| `data` | string | ID of the deleted review |

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
| `400` | Bad Request - Invalid parameters or missing required fields |
| `401` | Unauthorized - Invalid or missing authentication |
| `403` | Forbidden - Insufficient privileges or not review owner |
| `404` | Not Found - Review or product not found |
| `409` | Conflict - User already reviewed this product |
| `500` | Internal Server Error - Server error |
