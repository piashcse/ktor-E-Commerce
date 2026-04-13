# Shop Category API

This documentation provides comprehensive details for the Shop Category API endpoints. The API supports creating, retrieving, updating, and deleting shop categories. These endpoints are typically used by administrators to manage the categorization system for shops within the platform.

**Base URL:** `http://localhost:8080`

## Authentication

All Shop Category endpoints require Bearer token authentication with admin privileges. Include the access token in the Authorization header:

```
Authorization: Bearer <your_access_token>
```

### Shop Category Endpoints

| Method | Endpoint | Description | Authentication Required |
|--------|----------|-------------|------------------------|
| `POST` | `/shop-category` | Create a new shop category | Yes (Admin) |
| `GET` | `/shop-category` | Retrieve list of shop categories | Yes (Admin) |
| `PUT` | `/shop-category/{id}` | Update an existing shop category | Yes (Admin) |
| `DELETE` | `/shop-category/{id}` | Delete a shop category | Yes (Admin) |

---

## Endpoint Details

### 1. Create Shop Category

**`POST /shop-category`**

Create a new shop category. This endpoint allows administrators to add new categories for organizing shops.

#### Request Body

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `name` | string | Yes | Name of the shop category |

#### Headers

| Header | Value | Required |
|--------|-------|----------|
| `Authorization` | `Bearer <access_token>` | Yes |
| `Content-Type` | `application/json` | Yes |

#### Example Request

```bash
curl -X 'POST' \
  'http://localhost:8080/shop-category' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9...' \
  -H 'Content-Type: application/json' \
  -d '{
    "name": "New digital Shop"
  }'
```

#### Example Response

```json
{
    "id": "28918963-f932-425b-884b-a34d8ae69b2a",
    "name": "New digital Shop"
  }
}
```

#### Response Fields

| Field | Type | Description |
|-------|------|-------------|
| `id` | string | Unique identifier for the created shop category |
| `name` | string | Name of the shop category |

---

### 2. Get Shop Categories

**`GET /shop-category`**

Retrieve a list of shop categories with optional pagination support.

#### Query Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `limit` | number | No | Maximum number of categories to return (default: 10) |
| `offset` | number | No | Number of categories to skip for pagination |

#### Headers

| Header | Value | Required |
|--------|-------|----------|
| `Authorization` | `Bearer <access_token>` | Yes |

#### Example Request

```bash
curl -X 'GET' \
  'http://localhost:8080/shop-category?limit=10' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9...'
```

#### Example Response

```json
[
    {
      "id": "9c95c44c-3767-4ca2-9486-e28e390b3741",
      "name": "New Electronics"
    }
  ]
}
```

#### Response Fields

| Field | Type | Description |
|-------|------|-------------|
| `data` | array | Array of shop category objects |
| `data[].id` | string | Unique identifier for the shop category |
| `data[].name` | string | Name of the shop category |

---

### 3. Update Shop Category

**`PUT /shop-category/{id}`**

Update an existing shop category by its ID.

#### Path Parameters

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

#### Example Request

```bash
curl -X 'PUT' \
  'http://localhost:8080/shop-category/9c95c44c-3767-4ca2-9486-e28e390b3741?name=Piash%20Digital%20shop' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9...'
```

#### Example Response

```json
{
    "id": "9c95c44c-3767-4ca2-9486-e28e390b3741",
    "name": "Piash Digital shop"
  }
}
```

#### Response Fields

| Field | Type | Description |
|-------|------|-------------|
| `id` | string | Unique identifier of the updated shop category |
| `name` | string | Updated name of the shop category |

---

### 4. Delete Shop Category

**`DELETE /shop-category/{id}`**

Delete a shop category by its ID. This operation permanently removes the category from the system.

#### Path Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `id` | string | Yes | Unique identifier of the shop category to delete |

#### Headers

| Header | Value | Required |
|--------|-------|----------|
| `Authorization` | `Bearer <access_token>` | Yes |

#### Example Request

```bash
curl -X 'DELETE' \
  'http://localhost:8080/shop-category/9c95c44c-3767-4ca2-9486-e28e390b3741' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9...'
```

#### Example Response

```json
"2a17da31-7517-41db-b7d3-f77d0ddd52a5"
```

#### Response Fields

| Field | Type | Description |
|-------|------|-------------|
| `data` | string | ID of the deleted shop category |

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
