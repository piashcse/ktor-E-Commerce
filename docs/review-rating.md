# Review Rating

This documentation provides comprehensive details for the Review Rating API endpoints. The API supports creating, retrieving, updating, and deleting product reviews and ratings within the platform. Reviews are associated with specific products and users, enabling a complete feedback system for e-commerce platforms.

## Authentication

All Review Rating endpoints require Bearer token authentication. Include the access token in the Authorization header:

```
Authorization: Bearer <your_access_token>
```

### Review Rating Endpoints

| Method | Endpoint | Description | Authentication Required |
|--------|----------|-------------|------------------------|
| `GET` | `/api/v1/reviews` | Get reviews for a product (public) | No |
| `POST` | `/api/v1/reviews` | Create a New Review | Yes |
| `PUT` | `/api/v1/reviews/{id}` | Update Review | Yes |
| `DELETE` | `/api/v1/reviews/{id}` | Delete Review | Yes |
| `PUT` | `/api/v1/reviews/{id}/images` | Replace review images (max 8) | Yes |
| `POST` | `/api/v1/reviews/{id}/helpful` | Vote a review helpful | Yes |
| `GET` | `/api/v1/admin/reviews` | Admin: list reviews by status | Yes (Admin) |
| `PUT` | `/api/v1/admin/reviews/{id}/status` | Admin: moderate a review | Yes (Admin) |
| `POST` | `/api/v1/admin/reviews/bulk/status` | Admin: bulk moderate reviews | Yes |
| `GET` | `/api/v1/admin/reviews/export` | Admin: export reviews CSV | Yes |
| `GET` | `/api/v1/reviews/summary` | Rating distribution for a product | No |

---

## Endpoint Details

### 1. Get Reviews

**`GET /api/v1/reviews`**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `productId` | string | Yes | UUID of the product to get reviews for |
| `limit` | number | No | Maximum number of reviews to return (default: 20) |
| `offset` | number | No | Number of reviews to skip for pagination (default: 0) |

#### Headers

| Header | Value | Required |
|--------|-------|----------|
| `Authorization` | `Bearer <access_token>` | Yes |

#### Request

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/reviews?productId=cbd630f6-bf9f-48ad-ac51-f806807d99fd&limit=10' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9...'
```

#### Response

```json
{
  "data": [
    {
      "id": "70ac842b-7a81-4976-9564-d440880d1736",
      "userId": "a67fd0cc-3d92-4259-bbd4-1e0ba49dece4",
      "productId": "cbd630f6-bf9f-48ad-ac51-f806807d99fd",
      "reviewText": "Good product",
      "rating": 2
    }
  ],
  "metadata": {
    "totalCount": 1,
    "limit": 10,
    "offset": 0
  }
}
```

---

### 2. Create Review

**`POST /api/v1/reviews`**

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

#### Request

```bash
curl -X 'POST' \
  'http://localhost:8080/api/v1/reviews' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9...' \
  -H 'Content-Type: application/json' \
  -d '{
  "productId": "cbd630f6-bf9f-48ad-ac51-f806807d99fd",
  "rating": 2,
  "reviewText": "Good product"
}'
```

#### Response

```json
{
    "id": "70ac842b-7a81-4976-9564-d440880d1736",
    "userId": "a67fd0cc-3d92-4259-bbd4-1e0ba49dece4",
    "productId": "cbd630f6-bf9f-48ad-ac51-f806807d99fd",
    "reviewText": "Good product",
    "rating": 2
  }
}
```

---

### 3. Update Review

**`PUT /api/v1/reviews/{id}`**

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

#### Request

```bash
curl -X 'PUT' \
  'http://localhost:8080/api/v1/reviews/70ac842b-7a81-4976-9564-d440880d1736?review=Product%20review%20edited&rating=5' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9...'
```

#### Response

```json
{
    "id": "70ac842b-7a81-4976-9564-d440880d1736",
    "userId": "a67fd0cc-3d92-4259-bbd4-1e0ba49dece4",
    "productId": "cbd630f6-bf9f-48ad-ac51-f806807d99fd",
    "reviewText": "Product review edited",
    "rating": 5
  }
}
```

---

### 4. Delete Review

**`DELETE /api/v1/reviews/{id}`**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `id` | string | Yes | Unique identifier of the review to delete |

#### Headers

| Header | Value | Required |
|--------|-------|----------|
| `Authorization` | `Bearer <access_token>` | Yes |

#### Request

```bash
curl -X 'DELETE' \
  'http://localhost:8080/api/v1/reviews/70ac842b-7a81-4976-9564-d440880d1736' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9...'
```

#### Response

```json
{
  "message": "Review deleted successfully"
}
```

---

### 5. Vote Review Helpful

**`POST /api/v1/reviews/{id}/helpful`**


```bash
curl -X 'POST' \
  'http://localhost:8080/api/v1/reviews/6c5078d3-f8e3-4c88-9afe-48b5423c664f/helpful?helpful=true' \
  -H 'Authorization: Bearer <access_token>'
```

Returns the updated review with its `helpfulCount`.

### 6. Replace Review Images

**`PUT /api/v1/reviews/{id}/images`**
{
  "images": [
    "/uploads/review-images/front.png",
    "/uploads/review-images/side.png"
  ]
}
```

Only the review owner can replace images.

### 7. Admin Review Moderation

**`GET /api/v1/admin/reviews?status=FLAGGED`**

```json
{ "status": "HIDDEN" }
```

Hiding a review removes it from public product review lists and recalculates the product rating.

---

### 8. Admin: bulk moderate reviews

**`POST /api/v1/admin/reviews/bulk/status`**


#### Request

```bash
curl -X 'POST' \
  'http://localhost:8080/api/v1/admin/reviews/bulk/status?status=PENDING' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <admin_token>' \
  -H 'Content-Type: application/json' \
  -d '{
  "ids": [
    "6c5078d3-f8e3-4c88-9afe-48b5423c664f"
  ],
  "reason": "Policy violation"
}'
```

#### Response

```json
{
  "requested": 1,
  "succeeded": 1,
  "failed": []
}
```

---

### 9. Admin: export reviews CSV

**`GET /api/v1/admin/reviews/export`**

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/admin/reviews/export?status=PENDING' \
  -H 'accept: text/csv' \
  -H 'Authorization: Bearer <admin_token>'
```

#### Response

```csv
id,status,amount,currency,createdAt
example-id,PAID,250.00,USD,2024-05-06T12:00:00
```

---

### 10. Rating distribution for a product

**`GET /api/v1/reviews/summary`**

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/reviews/summary' \
  -H 'accept: application/json'
```

#### Response

```json
{
  "productId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "total": 1,
  "average": 250.0,
  "buckets": [
    {
      "stars": 1,
      "count": 1
    }
  ]
}
```

---

## Error Handling

This API follows industry-standard error handling patterns (Stripe, GitHub, OpenAI):

### Success Responses
- **HTTP status code indicates success** (200, 201, 204)
- **Response body contains data directly** (no wrapper object)
- No `isSuccess` or `statusCode` fields needed

#### Error Responses

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
