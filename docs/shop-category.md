# Shop Category

Create, update, list and remove shop categories.

## Authentication

All Shop Category endpoints require Bearer token authentication with admin privileges. Include the access token in the Authorization header:

```
Authorization: Bearer <your_access_token>
```

### Shop Category Endpoints

| Method | Endpoint | Description | Authentication Required |
|--------|----------|-------------|------------------------|
| `POST` | `/api/v1/admin/shop-categories` | Create a new shop category | Yes (Admin) |
| `GET` | `/api/v1/shop-categories` | Retrieve list of shop categories (public) | No |
| `PUT` | `/api/v1/admin/shop-categories/{id}` | Update an existing shop category | Yes (Admin) |
| `DELETE` | `/api/v1/admin/shop-categories/{id}` | Delete a shop category | Yes (Admin) |

---

## Endpoint Details

### 1. Create Shop Category

**`POST /api/v1/admin/shop-categories`**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `name` | string | Yes | Name of the shop category |

#### Headers

| Header | Value | Required |
|--------|-------|----------|
| `Authorization` | `Bearer <access_token>` | Yes |
| `Content-Type` | `application/json` | Yes |

#### Request

```bash
curl -X 'POST' \
  'http://localhost:8080/api/v1/admin/shop-categories' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9...' \
  -H 'Content-Type: application/json' \
  -d '{
    "name": "New digital Shop"
  }'
```

#### Response

```json
{
    "id": "28918963-f932-425b-884b-a34d8ae69b2a",
    "name": "New digital Shop"
  }
}
```

---

### 2. Get Shop Categories

**`GET /api/v1/shop-categories`**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `limit` | number | No | Maximum number of categories to return (default: 20) |
| `offset` | number | No | Number of categories to skip for pagination (default: 0) |

#### Headers

No authentication required (public).

#### Request

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/shop-categories?limit=10' \
  -H 'accept: application/json'
```

#### Response

```json
{
  "data": [
    {
      "id": "58f5c085-d04a-47de-beab-1d476b6ce432",
      "name": "Electronics"
    },
    {
      "id": "75b44e08-2c94-438f-b500-b204c7c90cca",
      "name": "Fashion"
    }
  ],
  "metadata": {
    "totalCount": 2,
    "limit": 10,
    "offset": 0
  }
}
```

---

### 3. Update Shop Category

**`PUT /api/v1/admin/shop-categories/{id}`**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `id` | string | Yes | Unique identifier of the shop category to update |

#### Query Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `name` | string | Yes | New name for the shop category |

#### Headers

| Header | Value | Required |
|--------|-------|----------|
| `Authorization` | `Bearer <access_token>` | Yes |

#### Request

```bash
curl -X 'PUT' \
  'http://localhost:8080/api/v1/admin/shop-categories/9c95c44c-3767-4ca2-9486-e28e390b3741?name=Piash%20Digital%20shop' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9...'
```

#### Response

```json
{
    "id": "9c95c44c-3767-4ca2-9486-e28e390b3741",
    "name": "Piash Digital shop"
  }
}
```

---

### 4. Delete Shop Category

**`DELETE /api/v1/admin/shop-categories/{id}`**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `id` | string | Yes | Unique identifier of the shop category to delete |

#### Headers

| Header | Value | Required |
|--------|-------|----------|
| `Authorization` | `Bearer <access_token>` | Yes |

#### Request

```bash
curl -X 'DELETE' \
  'http://localhost:8080/api/v1/admin/shop-categories/9c95c44c-3767-4ca2-9486-e28e390b3741' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9...'
```

#### Response

```json
{
  "message": "Shop category deleted successfully"
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
