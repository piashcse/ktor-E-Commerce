# Shop API

This documentation provides comprehensive details for the Shop API endpoints. The API supports creating, retrieving, updating, and deleting shops within the platform. Shops are associated with specific categories and can be managed by administrators or authorized users.

**Base URL:** `http://localhost:8080`

## Authentication

Most Shop endpoints require Bearer token authentication. Include the access token in the Authorization header:

```
Authorization: Bearer <your_access_token>
```

### Shop Endpoints

| Method | Endpoint | Description | Authentication Required |
|--------|----------|-------------|------------------------|
| `POST` | `/shop` | Create a new shop | Yes |
| `GET` | `/shop` | Retrieve list of shops | Yes |
| `PUT` | `/shop/{id}` | Update an existing shop | Yes |
| `DELETE` | `/shop/{id}` | Delete a shop | No |
| `GET` | `/shop/public` | Retrieve list of public shops with filters | No |
| `GET` | `/shop/category/{categoryId}` | Retrieve shops by category | No |
| `GET` | `/shop/featured` | Retrieve featured shops | No |
| `GET` | `/shop/status` | Retrieve shops by status | Yes (Admin/Super Admin) |
| `PUT` | `/shop/approve/{id}` | Approve a shop | Yes (Admin/Super Admin) |
| `PUT` | `/shop/reject/{id}` | Reject a shop | Yes (Admin/Super Admin) |
| `PUT` | `/shop/suspend/{id}` | Suspend a shop | Yes (Admin/Super Admin) |
| `PUT` | `/shop/activate/{id}` | Activate a shop | Yes (Admin/Super Admin) |

---

## Endpoint Details

### 1. Create Shop

**`POST /shop`**

Create a new shop with a specified name and category. The shop will be associated with a valid shop category.

#### Query Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `name` | string | Yes | Name of the shop |
| `categoryId` | string | Yes | UUID of the shop category to associate with |

#### Headers

| Header | Value | Required |
|--------|-------|----------|
| `Authorization` | `Bearer <access_token>` | Yes |

#### Example Request

```bash
curl -X 'POST' \
  'http://localhost:8080/shop?name=Royal%20Shop&categoryId=5e67ec97-9ed6-48ee-9d56-4163fe1711cb' \
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
    "id": "cbfdcfa3-fb65-4fa3-9078-e0f8cc63ddbc",
    "name": "Royal Shop"
  }
}
```

#### Response Fields

| Field | Type | Description |
|-------|------|-------------|
| `id` | string | Unique identifier for the created shop |
| `name` | string | Name of the shop |

---

### 2. Get Shops

**`GET /shop`**

Retrieve a list of shops with optional pagination support.

#### Query Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `limit` | number | No | Maximum number of shops to return (default: 10) |
| `offset` | number | No | Number of shops to skip for pagination |
| `categoryId` | string | No | Filter shops by category ID |

#### Headers

| Header | Value | Required |
|--------|-------|----------|
| `Authorization` | `Bearer <access_token>` | Yes |

#### Example Request

```bash
curl -X 'GET' \
  'http://localhost:8080/shop?limit=10' \
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
      "id": "a33b8912-e0b2-4058-9d7b-3c7ef9b935c7",
      "name": "Piash Shop update",
      "categoryId": "9c95c44c-3767-4ca2-9486-e28e390b3741"
    }
  ]
}
```

#### Response Fields

| Field | Type | Description |
|-------|------|-------------|
| `data` | array | Array of shop objects |
| `data[].id` | string | Unique identifier for the shop |
| `data[].name` | string | Name of the shop |
| `data[].categoryId` | string | UUID of the associated shop category |

---

### 3. Update Shop

**`PUT /shop/{id}`**

Update an existing shop by its ID. You can modify the shop name and optionally change its category.

#### Path Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `id` | string | Yes | Unique identifier of the shop to update |

#### Query Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `name` | string | No | New name for the shop |
| `categoryId` | string | No | New category ID to associate with the shop |

#### Headers

| Header | Value | Required |
|--------|-------|----------|
| `Authorization` | `Bearer <access_token>` | Yes |

#### Example Request

```bash
curl -X 'PUT' \
  'http://localhost:8080/shop/a33b8912-e0b2-4058-9d7b-3c7ef9b935c7?name=Shop%20again%20update' \
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
    "id": "a33b8912-e0b2-4058-9d7b-3c7ef9b935c7",
    "name": "Shop again update",
    "categoryId": "9c95c44c-3767-4ca2-9486-e28e390b3741"
  }
}
```

#### Response Fields

| Field | Type | Description |
|-------|------|-------------|
| `id` | string | Unique identifier of the updated shop |
| `name` | string | Updated name of the shop |
| `categoryId` | string | UUID of the associated shop category |

---

### 4. Delete Shop

**`DELETE /shop/{id}`**

Delete a shop by its ID. This operation permanently removes the shop from the system.

#### Path Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `id` | string | Yes | Unique identifier of the shop to delete |

#### Headers

| Header | Value | Required |
|--------|-------|----------|
| `Authorization` | `Bearer <access_token>` | No |

#### Example Request

```bash
curl -X 'DELETE' \
  'http://localhost:8080/shop/d2836959-6bc5-49d0-bd98-e73255a915c5' \
  -H 'accept: application/json'
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
    "id": "d2836959-6bc5-49d0-bd98-e73255a915c5"
  }
}
```

#### Response Fields

| Field | Type | Description |
|-------|------|-------------|
| `data.id` | string | ID of the deleted shop |

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
| `404` | Not Found - Shop or category not found |
| `409` | Conflict - Shop name already exists or invalid category |
| `500` | Internal Server Error - Server error |
