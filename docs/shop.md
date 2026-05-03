# Shop API

This documentation provides comprehensive details for the Shop API endpoints. The API supports creating, retrieving, updating, and deleting shops within the platform. Shops are associated with specific categories and can be managed by administrators or authorized users.

**Base URL:** `http://localhost:8080`

## Authentication

Most Shop endpoints require Bearer token authentication. Include the access token in the Authorization header:

```
Authorization: Bearer <your_access_token>
```

### Shop Endpoints (V1)

| Method | Endpoint | Description | Authentication Required |
|--------|----------|-------------|------------------------|
| `POST` | `/api/v1/seller/shop` | Create a new shop | Yes (Seller) |
| `GET` | `/api/v1/seller/shop` | Retrieve list of owned shops | Yes (Seller) |
| `PUT` | `/api/v1/seller/shop/{id}` | Update an existing shop | Yes (Seller) |
| `GET` | `/api/v1/shop/{id}` | Retrieve detailed info about a specific shop | No |
| `GET` | `/api/v1/shop/public` | Retrieve list of public shops with filters | No |
| `GET` | `/api/v1/shop/category/{categoryId}` | Retrieve shops by category | No |
| `GET` | `/api/v1/shop/featured` | Retrieve featured shops | No |
| `GET` | `/api/v1/admin/shop/status` | Retrieve shops by status | Yes (Admin) |
| `PUT` | `/api/v1/admin/shop/approve/{id}` | Approve a shop | Yes (Admin) |
| `PUT` | `/api/v1/admin/shop/reject/{id}` | Reject a shop | Yes (Admin) |
| `PUT` | `/api/v1/admin/shop/suspend/{id}` | Suspend a shop | Yes (Admin) |
| `PUT` | `/api/v1/admin/shop/activate/{id}` | Activate a shop | Yes (Admin) |

### Shop Endpoints (V2)

| Method | Endpoint | Description | Authentication Required |
|--------|----------|-------------|------------------------|
| `PUT` | `/api/v2/seller/shop/{shopId}` | Optimized shop update with source tracking | Yes (Seller) |

---

## Endpoint Details

### 1. Create Shop (V1)

**`POST /api/v1/seller/shop`**

Create a new shop with a specified name and category. The shop will be associated with the authenticated seller.

#### Headers

| Header | Value | Required |
|--------|-------|----------|
| `Authorization` | `Bearer <access_token>` | Yes |
| `Content-Type` | `application/json` | Yes |

#### Request Body

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `name` | string | Yes | Name of the shop |
| `categoryId` | string | Yes | UUID of the shop category |

#### Example Request

```bash
curl -X 'POST' \
  'http://localhost:8080/api/v1/seller/shop' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9...' \
  -H 'Content-Type: application/json' \
  -d '{
    "name": "Royal Shop",
    "categoryId": "5e67ec97-9ed6-48ee-9d56-4163fe1711cb"
  }'
```

#### Example Response

```json
{
    "id": "cbfdcfa3-fb65-4fa3-9078-e0f8cc63ddbc",
    "name": "Royal Shop"
}
```

---

### 2. Update Shop (V2 Optimized)

**`PUT /api/v2/seller/shop/{shopId}`**

Update an existing shop with enhanced metadata and a cleaner response structure.

#### Path Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `shopId` | string | Yes | Unique identifier of the shop to update |

#### Query Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `source` | string | No | Source of the update (e.g., 'mobile', 'web') |

#### Request Body

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `name` | string | No | New name for the shop |
| `categoryId` | string | No | New category ID |

#### Example Request

```bash
curl -X 'PUT' \
  'http://localhost:8080/api/v1/api/v2/seller/shop/a33b8912-e0b2-4058-9d7b-3c7ef9b935c7?source=mobile_app' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9...' \
  -H 'Content-Type: application/json' \
  -d '{
    "name": "Updated Shop V2"
  }'
```

#### Example Response

```json
{
    "v2_data": {
        "id": "a33b8912-e0b2-4058-9d7b-3c7ef9b935c7",
        "name": "Updated Shop V2",
        "categoryId": "9c95c44c-3767-4ca2-9486-e28e390b3741"
    },
    "source": "mobile_app"
}
```

---

### 3. Get Public Shops (V1)

**`GET /api/v1/shop/public`**

Retrieve a list of public shops with optional status and category filters.

#### Query Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `status` | string | No | Shop status to filter by (APPROVED, etc.) |
| `category` | string | No | UUID of the category to filter by |
| `limit` | number | No | Maximum number of shops to return |
| `offset` | number | No | Number of shops to skip |

#### Example Request

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/shop/public?status=APPROVED&limit=10' \
  -H 'accept: application/json'
```

#### Example Response

```json
{
  "data": [
    {
      "id": "a33b8912-e0b2-4058-9d7b-3c7ef9b935c7",
      "name": "Shop Name",
      "status": "APPROVED"
    }
  ],
  "metadata": {
    "totalCount": 1,
    "limit": 10,
    "skip": 0
  }
}
```

---

### 4. Admin Shop Approval

**`PUT /api/v1/admin/shop/approve/{id}`**

Approve a pending shop application (Admin only).

#### Example Request

```bash
curl -X 'PUT' \
  'http://localhost:8080/api/v1/admin/shop/approve/a33b8912-e0b2-4058-9d7b-3c7ef9b935c7' \
  -H 'Authorization: Bearer <admin_token>'
```

#### Example Response

```json
{
    "id": "a33b8912-e0b2-4058-9d7b-3c7ef9b935c7",
    "status": "APPROVED"
}
```

---

## Error Handling

### Success Responses
- **HTTP 200/201/204**: Operation successful. Data returned directly.

### Common Error Codes

| Status Code | Description |
|-------------|-------------|
| `400` | Bad Request (Validation failed) |
| `401` | Unauthorized (Invalid token) |
| `403` | Forbidden (Insufficient roles) |
| `404` | Not Found (Resource does not exist) |
| `410` | Gone (API version deprecated) |
| `500` | Internal Server Error |
