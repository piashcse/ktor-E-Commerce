# Product Sub Category API

This documentation provides comprehensive details for the Product Sub Category API endpoints. The API supports creating, retrieving, updating, and deleting product sub categories within the platform. Product sub categories are associated with specific product categories and can be managed by administrators or authorized users.

**Base URL:** `http://localhost:8080`

## Authentication

All Product Sub Category endpoints require Bearer token authentication. Include the access token in the Authorization header:

```
Authorization: Bearer <your_access_token>
```

### Product Sub Category Endpoints

| Method | Endpoint | Description | Authentication Required |
|--------|----------|-------------|------------------------|
| `POST` | `/product-subcategory` | Create a new product sub category | Yes |
| `GET` | `/product-subcategory` | Retrieve list of product sub categories | Yes |
| `PUT` | `/product-subcategory/{id}` | Update an existing product sub category | Yes |
| `DELETE` | `/product-subcategory/{id}` | Delete a product sub category | Yes |

---

## Endpoint Details

### 1. Create Product Sub Category

**`POST /product-subcategory`**

Create a new product sub category with a specified name and associated product category.

#### Request Body

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `name` | string | Yes | Name of the product sub category |
| `categoryId` | string | Yes | UUID of the product category to associate with |

#### Headers

| Header | Value | Required |
|--------|-------|----------|
| `Authorization` | `Bearer <access_token>` | Yes |
| `Content-Type` | `application/json` | Yes |

#### Example Request

```bash
curl -X 'POST' \
  'http://localhost:8080/product-subcategory' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9...' \
  -H 'Content-Type: application/json' \
  -d '{
  "categoryId": "b8ccc13f-e118-4540-8e9e-5eaa8028cb4f",
  "name": "Paper"
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
    "id": "751cef10-f98a-4ecc-ae03-4173830a626d",
    "categoryId": "b8ccc13f-e118-4540-8e9e-5eaa8028cb4f",
    "name": "Paper"
  }
}
```

#### Response Fields

| Field | Type | Description |
|-------|------|-------------|
| `id` | string | Unique identifier for the created product sub category |
| `categoryId` | string | UUID of the associated product category |
| `name` | string | Name of the product sub category |

---

### 2. Get Product Sub Categories

**`GET /product-subcategory`**

Retrieve a list of product sub categories with optional filtering and pagination support.

#### Query Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `id` | string | No | Filter by specific category ID (returns sub categories for that category) |
| `limit` | number | No | Maximum number of sub categories to return (default: 10) |
| `offset` | number | No | Number of sub categories to skip for pagination |

#### Headers

| Header | Value | Required |
|--------|-------|----------|
| `Authorization` | `Bearer <access_token>` | Yes |

#### Example Request

```bash
curl -X 'GET' \
  'http://localhost:8080/product-subcategory?id=b8ccc13f-e118-4540-8e9e-5eaa8028cb4f&limit=10' \
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
      "id": "751cef10-f98a-4ecc-ae03-4173830a626d",
      "categoryId": "b8ccc13f-e118-4540-8e9e-5eaa8028cb4f",
      "name": "Paper"
    }
  ]
}
```

#### Response Fields

| Field | Type | Description |
|-------|------|-------------|
| `data` | array | Array of product sub category objects |
| `data[].id` | string | Unique identifier for the product sub category |
| `data[].categoryId` | string | UUID of the associated product category |
| `data[].name` | string | Name of the product sub category |

---

### 3. Update Product Sub Category

**`PUT /product-subcategory/{id}`**

Update an existing product sub category by its ID. You can modify the sub category name.

#### Path Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `id` | string | Yes | Unique identifier of the product sub category to update |

#### Query Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `name` | string | Yes | New name for the product sub category |

#### Headers

| Header | Value | Required |
|--------|-------|----------|
| `Authorization` | `Bearer <access_token>` | Yes |

#### Example Request

```bash
curl -X 'PUT' \
  'http://localhost:8080/product-subcategory/751cef10-f98a-4ecc-ae03-4173830a626d?name=Pencil' \
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
    "id": "751cef10-f98a-4ecc-ae03-4173830a626d",
    "categoryId": "b8ccc13f-e118-4540-8e9e-5eaa8028cb4f",
    "name": "Pencil"
  }
}
```

#### Response Fields

| Field | Type | Description |
|-------|------|-------------|
| `id` | string | Unique identifier of the updated product sub category |
| `categoryId` | string | UUID of the associated product category |
| `name` | string | Updated name of the product sub category |

---

### 4. Delete Product Sub Category

**`DELETE /product-subcategory/{id}`**

Delete a product sub category by its ID. This operation permanently removes the sub category from the system.

#### Path Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `id` | string | Yes | Unique identifier of the product sub category to delete |

#### Headers

| Header | Value | Required |
|--------|-------|----------|
| `Authorization` | `Bearer <access_token>` | Yes |

#### Example Request

```bash
curl -X 'DELETE' \
  'http://localhost:8080/product-subcategory/751cef10-f98a-4ecc-ae03-4173830a626d' \
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
  "data": "751cef10-f98a-4ecc-ae03-4173830a626d"
}
```

#### Response Fields

| Field | Type | Description |
|-------|------|-------------|
| `data` | string | ID of the deleted product sub category |

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
| `403` | Forbidden - Insufficient privileges |
| `404` | Not Found - Product sub category or category not found |
| `409` | Conflict - Sub category name already exists or invalid category |
| `500` | Internal Server Error - Server error |
