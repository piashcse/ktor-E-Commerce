# Shop Public API

This documentation provides comprehensive details for the Shop Public API endpoints. The API supports retrieving public shop information with various filters, allowing customers to discover shops based on status, category, and other attributes. These endpoints are designed to be accessible to all users without requiring authentication.

**Base URL:** `http://localhost:8080`

## Authentication

Shop public endpoints do not require authentication, allowing public access to shop information.

### Shop Public Endpoints

| Method | Endpoint | Description | Authentication Required |
|--------|----------|-------------|------------------------|
| `GET` | `/shop/public` | Retrieve public shops with filters | No |
| `GET` | `/shop/category/{categoryId}` | Retrieve shops by category | No |
| `GET` | `/shop/featured` | Retrieve featured shops | No |
| `GET` | `/shop/status` | Retrieve shops by status | No |
| `PUT` | `/shop/approve/{id}` | Approve a shop (Admin/Super Admin) | Yes |
| `PUT` | `/shop/reject/{id}` | Reject a shop (Admin/Super Admin) | Yes |
| `PUT` | `/shop/suspend/{id}` | Suspend a shop (Admin/Super Admin) | Yes |
| `PUT` | `/shop/activate/{id}` | Activate a shop (Admin/Super Admin) | Yes |

---

## Endpoint Details

### 1. Get Public Shops

**`GET /shop/public`**

Retrieve a list of public shops with optional status and category filters.

#### Query Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `status` | string | No | Shop status to filter by (APPROVED, etc.) |
| `category` | string | No | UUID of the category to filter by |
| `limit` | number | No | Maximum number of shops to return (default: 10) |

#### Headers

| Header | Value | Required |
|--------|-------|----------|
| `accept` | `application/json` | Yes |

#### Example Request

```bash
curl -X 'GET' \
  'http://localhost:8080/shop/public?status=APPROVED&category=5e67ec97-9ed6-48ee-9d56-4163fe1711cb&limit=10' \
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
  "data": [
    {
      "id": "a33b8912-e0b2-4058-9d7b-3c7ef9b935c7",
      "name": "Shop Name",
      "categoryId": "5e67ec97-9ed6-48ee-9d56-4163fe1711cb",
      "status": "APPROVED"
    }
  ]
}
```

#### Response Fields

| Field | Type | Description |
|-------|------|-------------|
| `data` | array | Array of shop objects matching the criteria |
| `data[].id` | string | Unique identifier for the shop |
| `data[].name` | string | Name of the shop |
| `data[].categoryId` | string | UUID of the category the shop belongs to |
| `data[].status` | string | Status of the shop (APPROVED, REJECTED, SUSPENDED, etc.) |

---

### 2. Get Shops by Category

**`GET /shop/category/{categoryId}`**

Retrieve shops belonging to a specific category.

#### Path Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `categoryId` | string | Yes | UUID of the category to get shops for |

#### Headers

| Header | Value | Required |
|--------|-------|----------|
| `accept` | `application/json` | Yes |

#### Example Request

```bash
curl -X 'GET' \
  'http://localhost:8080/shop/category/5e67ec97-9ed6-48ee-9d56-4163fe1711cb' \
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
  "data": [
    {
      "id": "a33b8912-e0b2-4058-9d7b-3c7ef9b935c7",
      "name": "Shop Name",
      "categoryId": "5e67ec97-9ed6-48ee-9d56-4163fe1711cb",
      "status": "APPROVED"
    }
  ]
}
```

#### Response Fields

| Field | Type | Description |
|-------|------|-------------|
| `data` | array | Array of shop objects in the category |
| `data[].id` | string | Unique identifier for the shop |
| `data[].name` | string | Name of the shop |
| `data[].categoryId` | string | UUID of the category the shop belongs to |
| `data[].status` | string | Status of the shop |

---

### 3. Get Featured Shops

**`GET /shop/featured`**

Retrieve a list of featured shops.

#### Headers

| Header | Value | Required |
|--------|-------|----------|
| `accept` | `application/json` | Yes |

#### Example Request

```bash
curl -X 'GET' \
  'http://localhost:8080/shop/featured' \
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
  "data": [
    {
      "id": "a33b8912-e0b2-4058-9d7b-3c7ef9b935c7",
      "name": "Featured Shop",
      "categoryId": "5e67ec97-9ed6-48ee-9d56-4163fe1711cb",
      "status": "APPROVED"
    }
  ]
}
```

#### Response Fields

| Field | Type | Description |
|-------|------|-------------|
| `data` | array | Array of featured shop objects |
| `data[].id` | string | Unique identifier for the shop |
| `data[].name` | string | Name of the shop |
| `data[].categoryId` | string | UUID of the category the shop belongs to |
| `data[].status` | string | Status of the shop |

---

### 4. Get Shops by Status

**`GET /shop/status`**

Retrieve shops filtered by status.

#### Query Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `status` | string | Yes | Status to filter shops by (APPROVED, REJECTED, SUSPENDED, etc.) |

#### Headers

| Header | Value | Required |
|--------|-------|----------|
| `accept` | `application/json` | Yes |
| `Authorization` | `Bearer <access_token>` | Yes (for admin access) |

#### Example Request

```bash
curl -X 'GET' \
  'http://localhost:8080/shop/status?status=APPROVED' \
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
      "name": "Approved Shop",
      "categoryId": "5e67ec97-9ed6-48ee-9d56-4163fe1711cb",
      "status": "APPROVED"
    }
  ]
}
```

#### Response Fields

| Field | Type | Description |
|-------|------|-------------|
| `data` | array | Array of shop objects with the specified status |
| `data[].id` | string | Unique identifier for the shop |
| `data[].name` | string | Name of the shop |
| `data[].categoryId` | string | UUID of the category the shop belongs to |
| `data[].status` | string | Status of the shop |

---

### 5. Approve Shop

**`PUT /shop/approve/{id}`**

Approve a shop (Admin/Super Admin only).

#### Path Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `id` | string | Yes | Unique identifier of the shop to approve |

#### Headers

| Header | Value | Required |
|--------|-------|----------|
| `accept` | `application/json` | Yes |
| `Authorization` | `Bearer <access_token>` | Yes (Admin/Super Admin) |

#### Example Request

```bash
curl -X 'PUT' \
  'http://localhost:8080/shop/approve/a33b8912-e0b2-4058-9d7b-3c7ef9b935c7' \
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
    "name": "Approved Shop",
    "status": "APPROVED"
  }
}
```

#### Response Fields

| Field | Type | Description |
|-------|------|-------------|
| `data.id` | string | Unique identifier of the approved shop |
| `data.name` | string | Name of the approved shop |
| `data.status` | string | New status of the shop (APPROVED) |

---

### 6. Reject Shop

**`PUT /shop/reject/{id}`**

Reject a shop (Admin/Super Admin only).

#### Path Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `id` | string | Yes | Unique identifier of the shop to reject |

#### Headers

| Header | Value | Required |
|--------|-------|----------|
| `accept` | `application/json` | Yes |
| `Authorization` | `Bearer <access_token>` | Yes (Admin/Super Admin) |

#### Example Request

```bash
curl -X 'PUT' \
  'http://localhost:8080/shop/reject/a33b8912-e0b2-4058-9d7b-3c7ef9b935c7' \
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
    "name": "Rejected Shop",
    "status": "REJECTED"
  }
}
```

#### Response Fields

| Field | Type | Description |
|-------|------|-------------|
| `data.id` | string | Unique identifier of the rejected shop |
| `data.name` | string | Name of the rejected shop |
| `data.status` | string | New status of the shop (REJECTED) |

---

### 7. Suspend Shop

**`PUT /shop/suspend/{id}`**

Suspend a shop (Admin/Super Admin only).

#### Path Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `id` | string | Yes | Unique identifier of the shop to suspend |

#### Headers

| Header | Value | Required |
|--------|-------|----------|
| `accept` | `application/json` | Yes |
| `Authorization` | `Bearer <access_token>` | Yes (Admin/Super Admin) |

#### Example Request

```bash
curl -X 'PUT' \
  'http://localhost:8080/shop/suspend/a33b8912-e0b2-4058-9d7b-3c7ef9b935c7' \
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
    "name": "Suspended Shop",
    "status": "SUSPENDED"
  }
}
```

#### Response Fields

| Field | Type | Description |
|-------|------|-------------|
| `data.id` | string | Unique identifier of the suspended shop |
| `data.name` | string | Name of the suspended shop |
| `data.status` | string | New status of the shop (SUSPENDED) |

---

### 8. Activate Shop

**`PUT /shop/activate/{id}`**

Activate a shop (Admin/Super Admin only).

#### Path Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `id` | string | Yes | Unique identifier of the shop to activate |

#### Headers

| Header | Value | Required |
|--------|-------|----------|
| `accept` | `application/json` | Yes |
| `Authorization` | `Bearer <access_token>` | Yes (Admin/Super Admin) |

#### Example Request

```bash
curl -X 'PUT' \
  'http://localhost:8080/shop/activate/a33b8912-e0b2-4058-9d7b-3c7ef9b935c7' \
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
    "name": "Activated Shop",
    "status": "APPROVED"
  }
}
```

#### Response Fields

| Field | Type | Description |
|-------|------|-------------|
| `data.id` | string | Unique identifier of the activated shop |
| `data.name` | string | Name of the activated shop |
| `data.status` | string | New status of the shop (APPROVED) |

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
| `400` | Bad Request - Invalid parameters or search criteria |
| `401` | Unauthorized - Invalid or missing authentication |
| `403` | Forbidden - Insufficient privileges (not admin/super admin) |
| `404` | Not Found - Shop or category not found |
| `500` | Internal Server Error - Server error |

---

## Shop Management Guidelines

### Public Access
- Public endpoints provide access to APPROVED shops only
- Featured shops are highlighted based on admin configuration
- Category-based filtering helps users discover relevant shops

### Administrative Actions
- Approval/rejection affects shop visibility to customers
- Suspended shops are temporarily unavailable
- Activated shops return to approved state after suspension