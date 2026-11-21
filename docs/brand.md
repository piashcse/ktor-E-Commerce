# Brand

Create, update, list and remove product brands; upload brand logos.

## Authentication

Brand listing (`GET /api/v1/brands`) is public. Brand management endpoints require Bearer token authentication.

```
Authorization: Bearer <your_access_token>
```
### Brand Endpoints

| Method | Endpoint | Description | Authentication Required |
|--------|----------|-------------|------------------------|
| `POST` | `/api/v1/admin/brands` | Create a new brand | Yes (Admin) |
| `GET` | `/api/v1/brands` | Retrieve list of brands | No (public) |
| `PUT` | `/api/v1/admin/brands/{id}` | Update an existing brand | Yes (Admin) |
| `POST` | `/api/v1/admin/brands/{id}/image` | Upload a brand logo | Yes (Admin) |
| `DELETE` | `/api/v1/admin/brands/{id}` | Delete a brand | Yes (Admin) |

---

## Endpoint Details

### 1. Create Brand

**`POST /api/v1/admin/brands`**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `name` | string | Yes | Name of the brand |

#### Headers

| Header | Value | Required |
|--------|-------|----------|
| `Authorization` | `Bearer <access_token>` | Yes |

#### Request

```bash
curl -X 'POST' \
  'http://localhost:8080/api/v1/admin/brands' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9...' \
  -d ''
```

#### Response

```json
{
    "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
    "name": "Nike"
  }
}
```

---

### 2. Get Brands

**`GET /api/v1/brands`**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `limit` | number | No | Maximum number of brands to return (default: 20) |
| `offset` | number | No | Number of brands to skip for pagination (default: 0) |

#### Headers

No authentication required (public).

#### Request

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/brands?limit=10' \
  -H 'accept: application/json'
```

#### Response

```json
{
  "data": [
    {
      "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "name": "Nike"
    },
    {
      "id": "19dd1021-432c-473c-8b19-0f56d19af9ad",
      "name": "PUMA"
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

### 3. Update Brand

**`PUT /api/v1/admin/brands/{id}`**

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

#### Request

```bash
curl -X 'PUT' \
  'http://localhost:8080/api/v1/admin/brands/6c5078d3-f8e3-4c88-9afe-48b5423c664f?name=Addidas' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9...'
```

#### Response

```json
{
    "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
    "name": "Addidas"
  }
}
```

---

### 4. Upload Brand Logo

**`POST /api/v1/admin/brands/{id}/image`**

#### Multipart Form Data

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `image` | file | Yes | `image/jpeg`, `image/png` or `image/webp` (`.jpg`, `.jpeg`, `.png`, `.webp`). Maximum **5 MB**. |

#### Headers

| Header | Value | Required |
|--------|-------|----------|
| `Authorization` | `Bearer <admin_token>` | Yes |
| `Content-Type` | `multipart/form-data` | Yes |

#### Request

```bash
curl -X 'POST' \
  'http://localhost:8080/api/v1/admin/brands/{id}/image' \
  -H 'Authorization: Bearer <admin_token>' \
  -H 'Content-Type: multipart/form-data' \
  -F 'image=@logo.png;type=image/png'
```

#### Response

```json
{
  "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "name": "Nike",
  "logo": "/uploads/brand-images/8f2c1a.png"
}
```

---

### 5. Delete Brand

**`DELETE /api/v1/admin/brands/{id}`**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `id` | string | Yes | Unique identifier of the brand to delete |

#### Headers

| Header | Value | Required |
|--------|-------|----------|
| `Authorization` | `Bearer <access_token>` | Yes |

#### Request

```bash
curl -X 'DELETE' \
  'http://localhost:8080/api/v1/admin/brands/19dd1021-432c-473c-8b19-0f56d19af9ad' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9...'
```

#### Response

```json
{
  "message": "Brand deleted successfully"
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
