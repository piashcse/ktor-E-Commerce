# Product Category API

This documentation provides comprehensive details for the Product Category API endpoints. The API supports creating, retrieving, updating, and deleting product categories within the platform. Product categories can have sub-categories and are managed by administrators or authorized users.

**Base URL:** `http://localhost:8080`

## Authentication

All Product Category endpoints require Bearer token authentication. Include the access token in the Authorization header:

```
Authorization: Bearer <your_access_token>
```

### Product Category Endpoints

| Method | Endpoint | Description | Authentication Required |
|--------|----------|-------------|------------------------|
| `POST` | `/product-category` | Create a new product category | Yes |
| `GET` | `/product-category` | Retrieve list of product categories | Yes |
| `PUT` | `/product-category/{id}` | Update an existing product category | Yes |
| `DELETE` | `/product-category/{id}` | Delete a product category | Yes |

---

## Endpoint Details

### 1. Create Product Category

**`POST /product-category`**

Create a new product category with a specified name. The category will be created with an empty sub-categories array.

#### Query Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `categoryName` | string | Yes | Name of the product category |

#### Headers

| Header | Value | Required |
|--------|-------|----------|
| `Authorization` | `Bearer <access_token>` | Yes |

#### Example Request

```bash
curl -X 'POST' \
  'http://localhost:8080/product-category?categoryName=Kids' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9...' \
  -d ''
```

#### Example Response

```json
{
    "id": "75b44e08-2c94-438f-b500-b204c7c90cca",
    "name": "Kids",
    "subCategories": []
  }
}
```

#### Response Fields

| Field | Type | Description |
|-------|------|-------------|
| `id` | string | Unique identifier for the created product category |
| `name` | string | Name of the product category |
| `subCategories` | array | Array of sub-categories (empty for new categories) |

---

### 2. Get Product Categories

**`GET /product-category`**

Retrieve a list of product categories with optional pagination support.

#### Query Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `limit` | number | No | Maximum number of categories to return (default: 20) |
| `offset` | number | No | Number of categories to skip for pagination (default: 0) |

#### Headers

| Header | Value | Required |
|--------|-------|----------|
| `Authorization` | `Bearer <access_token>` | Yes |

#### Example Request

```bash
curl -X 'GET' \
  'http://localhost:8080/product-category?limit=10' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9...'
```

#### Example Response

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
    "skip": 0
  }
}
```

#### Response Fields

| Field | Type | Description |
|-------|------|-------------|
| `data` | array | Array of product category objects |
| `data[].id` | string | Unique identifier for the product category |
| `data[].name` | string | Name of the product category |
| `data[].subCategories` | array | Array of sub-categories associated with the category |

---

### 3. Update Product Category

**`PUT /product-category/{id}`**

Update an existing product category by its ID. You can modify the category name.

#### Path Parameters

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

#### Example Request

```bash
curl -X 'PUT' \
  'http://localhost:8080/product-category/b8ccc13f-e118-4540-8e9e-5eaa8028cb4f?name=Education%203.0' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9...'
```

#### Example Response

```json
{
    "id": "b8ccc13f-e118-4540-8e9e-5eaa8028cb4f",
    "name": "Education 3.0",
    "subCategories": []
  }
}
```

#### Response Fields

| Field | Type | Description |
|-------|------|-------------|
| `id` | string | Unique identifier of the updated product category |
| `name` | string | Updated name of the product category |
| `subCategories` | array | Array of sub-categories associated with the category |

---

### 4. Delete Product Category

**`DELETE /product-category/{id}`**

Delete a product category by its ID. This operation permanently removes the category from the system.

#### Path Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `id` | string | Yes | Unique identifier of the product category to delete |

#### Headers

| Header | Value | Required |
|--------|-------|----------|
| `Authorization` | `Bearer <access_token>` | Yes |

#### Example Request

```bash
curl -X 'DELETE' \
  'http://localhost:8080/product-category/75b44e08-2c94-438f-b500-b204c7c90cca' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9...'
```

#### Example Response

```json
"75b44e08-2c94-438f-b500-b204c7c90cca"
```

#### Response Fields

| Field | Type | Description |
|-------|------|-------------|
| `data` | string | ID of the deleted product category |

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
