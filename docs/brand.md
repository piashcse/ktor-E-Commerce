# Brand API

This documentation provides comprehensive details for the Brand API endpoints. The API supports creating, retrieving, updating, and deleting brands within the platform. Brands are independent entities that can be associated with products and managed by administrators or authorized users.

**Base URL:** `http://localhost:8080`

## Authentication

All Brand endpoints require Bearer token authentication. Include the access token in the Authorization header:

```
Authorization: Bearer <your_access_token>
```
### Brand Endpoints

| Method | Endpoint | Description | Authentication Required |
|--------|----------|-------------|------------------------|
| `POST` | `/brand` | Create a new brand | Yes |
| `GET` | `/brand` | Retrieve list of brands | Yes |
| `PUT` | `/brand/{id}` | Update an existing brand | Yes |
| `DELETE` | `/brand/{id}` | Delete a brand | Yes |

---

## Endpoint Details

### 1. Create Brand

**`POST /brand`**

Create a new brand with a specified name.

#### Query Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `name` | string | Yes | Name of the brand |

#### Headers

| Header | Value | Required |
|--------|-------|----------|
| `Authorization` | `Bearer <access_token>` | Yes |

#### Example Request

```bash
curl -X 'POST' \
  'http://localhost:8080/brand?name=Nike' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9...' \
  -d ''
```

#### Example Response

```json
{
    "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
    "name": "Nike"
  }
}
```

#### Response Fields

| Field | Type | Description |
|-------|------|-------------|
| `id` | string | Unique identifier for the created brand |
| `name` | string | Name of the brand |

---

### 2. Get Brands

**`GET /brand`**

Retrieve a list of brands with optional pagination support.

#### Query Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `limit` | number | No | Maximum number of brands to return (default: 10) |
| `offset` | number | No | Number of brands to skip for pagination |

#### Headers

| Header | Value | Required |
|--------|-------|----------|
| `Authorization` | `Bearer <access_token>` | Yes |

#### Example Request

```bash
curl -X 'GET' \
  'http://localhost:8080/brand?limit=10' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9...'
```

#### Example Response

```json
[
    {
      "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "name": "Nike"
    },
    {
      "id": "19dd1021-432c-473c-8b19-0f56d19af9ad",
      "name": "PUMA"
    }
  ]
}
```

#### Response Fields

| Field | Type | Description |
|-------|------|-------------|
| `data` | array | Array of brand objects |
| `data[].id` | string | Unique identifier for the brand |
| `data[].name` | string | Name of the brand |

---

### 3. Update Brand

**`PUT /brand/{id}`**

Update an existing brand by its ID. You can modify the brand name.

#### Path Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `id` | string | Yes | Unique identifier of the brand to update |

#### Query Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `name` | string | Yes | New name for the brand |

#### Headers

| Header | Value | Required |
|--------|-------|----------|
| `Authorization` | `Bearer <access_token>` | Yes |

#### Example Request

```bash
curl -X 'PUT' \
  'http://localhost:8080/brand/6c5078d3-f8e3-4c88-9afe-48b5423c664f?name=Addidas' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9...'
```

#### Example Response

```json
{
    "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
    "name": "Addidas"
  }
}
```

#### Response Fields

| Field | Type | Description |
|-------|------|-------------|
| `id` | string | Unique identifier of the updated brand |
| `name` | string | Updated name of the brand |

---

### 4. Delete Brand

**`DELETE /brand/{id}`**

Delete a brand by its ID. This operation permanently removes the brand from the system.

#### Path Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `id` | string | Yes | Unique identifier of the brand to delete |

#### Headers

| Header | Value | Required |
|--------|-------|----------|
| `Authorization` | `Bearer <access_token>` | Yes |

#### Example Request

```bash
curl -X 'DELETE' \
  'http://localhost:8080/brand/19dd1021-432c-473c-8b19-0f56d19af9ad' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9...'
```

#### Example Response

```json
"19dd1021-432c-473c-8b19-0f56d19af9ad"
```

#### Response Fields

| Field | Type | Description |
|-------|------|-------------|
| `data` | string | ID of the deleted brand |

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
