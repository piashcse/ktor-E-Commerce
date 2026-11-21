# Product Category

This documentation provides comprehensive details for the Product Category API endpoints. The API supports creating, retrieving, updating, and deleting product categories within the platform. Product categories can have sub-categories and are managed by administrators or authorized users.

## Authentication

This documentation provides comprehensive details for the Product Category API endpoints. The
public listing endpoint is unauthenticated; create/update/delete and image upload require an admin token.

```
Authorization: Bearer <your_access_token>
```

### Product Category Endpoints

| Method | Endpoint | Description | Authentication Required |
|--------|----------|-------------|------------------------|
| `POST` | `/api/v1/admin/product-categories` | Create a new product category | Yes (Admin) |
| `GET` | `/api/v1/product-categories` | Retrieve list of product categories | No (public) |
| `PUT` | `/api/v1/admin/product-categories/{id}` | Update an existing product category | Yes (Admin) |
| `POST` | `/api/v1/admin/product-categories/{id}/image` | Upload a category image | Yes (Admin) |
| `DELETE` | `/api/v1/admin/product-categories/{id}` | Delete a product category | Yes (Admin) |

---

## Endpoint Details

### 1. Create Product Category

**`POST /api/v1/admin/product-categories`**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `name` | string | Yes | Name of the product category |

#### Headers

| Header | Value | Required |
|--------|-------|----------|
| `Authorization` | `Bearer <access_token>` | Yes |

#### Request

```bash
curl -X 'POST' \
  'http://localhost:8080/api/v1/admin/product-categories?name=Kids' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9...' \
  -d ''
```

#### Response

```json
{
    "id": "75b44e08-2c94-438f-b500-b204c7c90cca",
    "name": "Kids",
    "subCategories": []
  }
}
```

---

### 2. Get Product Categories

**`GET /api/v1/product-categories`**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `limit` | number | No | Maximum number of categories to return (default: 20) |
| `offset` | number | No | Number of categories to skip for pagination (default: 0) |

#### Headers

No authentication required (public).

#### Request

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/product-categories?limit=10' \
  -H 'accept: application/json'
```

#### Response

```json
{
  "data": [
    {
      "id": "58f5c085-d04a-47de-beab-1d476b6ce432",
      "name": "Mens Cloth",
      "subCategories": []
    },
    {
      "id": "75b44e08-2c94-438f-b500-b204c7c90cca",
      "name": "Kids",
      "subCategories": []
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

### 3. Update Product Category

**`PUT /api/v1/admin/product-categories/{id}`**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `id` | string | Yes | Unique identifier of the product category to update |

#### Query Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `name` | string | Yes | New name for the product category |

#### Headers

| Header | Value | Required |
|--------|-------|----------|
| `Authorization` | `Bearer <access_token>` | Yes |

#### Request

```bash
curl -X 'PUT' \
  'http://localhost:8080/api/v1/admin/product-categories/b8ccc13f-e118-4540-8e9e-5eaa8028cb4f?name=Education%203.0' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9...'
```

#### Response

```json
{
    "id": "b8ccc13f-e118-4540-8e9e-5eaa8028cb4f",
    "name": "Education 3.0",
    "subCategories": []
  }
}
```

---

### 4. Delete Product Category

**`DELETE /api/v1/admin/product-categories/{id}`**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `id` | string | Yes | Unique identifier of the product category to delete |

#### Headers

| Header | Value | Required |
|--------|-------|----------|
| `Authorization` | `Bearer <access_token>` | Yes |

#### Request

```bash
curl -X 'DELETE' \
  'http://localhost:8080/api/v1/admin/product-categories/75b44e08-2c94-438f-b500-b204c7c90cca' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9...'
```

#### Response

```json
"75b44e08-2c94-438f-b500-b204c7c90cca"
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

### 5. Upload Category Image

**`POST /api/v1/admin/product-categories/{id}/image`**

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
  'http://localhost:8080/api/v1/admin/product-categories/{id}/image' \
  -H 'Authorization: Bearer <admin_token>' \
  -H 'Content-Type: multipart/form-data' \
  -F 'image=@category.png;type=image/png'
```
