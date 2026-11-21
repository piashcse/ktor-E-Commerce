# Shop

Shop onboarding, profiles, images, approval and staff management.

## Authentication

Most Shop endpoints require Bearer token authentication. Include the access token in the Authorization header:

```
Authorization: Bearer <your_access_token>
```

### Shop Endpoints (V1)

| Method | Endpoint | Description | Authentication Required |
|--------|----------|-------------|------------------------|
| `POST` | `/api/v1/seller/shops` | Create a new shop | Yes (Seller) |
| `GET` | `/api/v1/seller/shops` | Retrieve list of owned shops | Yes (Seller) |
| `PUT` | `/api/v1/seller/shops/{id}` | Update an existing shop | Yes (Seller) |
| `POST` | `/api/v1/seller/shops/{id}/image` | Upload a shop logo | Yes (Seller) |
| `GET` | `/api/v1/shops/{id}` | Retrieve detailed info about a specific shop | No |
| `GET` | `/api/v1/shops/public` | Retrieve list of public shops with filters | No |
| `GET` | `/api/v1/shops/category/{categoryId}` | Retrieve shops by category | No |
| `GET` | `/api/v1/shops/featured` | Retrieve featured shops | No |
| `GET` | `/api/v1/admin/shops/status` | Retrieve shops by status | Yes (Admin) |
| `PUT` | `/api/v1/admin/shops/approve/{id}` | Approve a shop | Yes (Admin) |
| `PUT` | `/api/v1/admin/shops/reject/{id}` | Reject a shop | Yes (Admin) |
| `PUT` | `/api/v1/admin/shops/suspend/{id}` | Suspend a shop | Yes (Admin) |
| `PUT` | `/api/v1/admin/shops/activate/{id}` | Activate a shop | Yes (Admin) |
| `GET` | `/api/v1/admin/shops` | Admin - List all shops | Yes |
| `POST` | `/api/v1/admin/shops/bulk/approve` | Admin - Bulk approve shops | Yes |
| `POST` | `/api/v1/admin/shops/bulk/reject` | Admin - Bulk reject shops | Yes |
| `POST` | `/api/v1/admin/shops/bulk/suspend` | Admin - Bulk suspend shops | Yes |
| `GET` | `/api/v1/shops/{id}/products` | List products for a shop | No |

---

## Shop staff

Shop owners can invite staff and assign shop roles:

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/v1/seller/members` | List shop staff |
| `POST` | `/api/v1/seller/members/invite` | Invite a staff member |
| `PUT` | `/api/v1/seller/members/{id}/roles` | Replace a member's roles |
| `DELETE` | `/api/v1/seller/members/{id}` | Remove a member |

Shop staff roles include owner, manager, catalog, fulfillment, inventory, finance, support,
analyst, warehouse and POS. See [RBAC](rbac.md) for the full role catalog.

---

## Endpoint Details

### 1. Create Shop (V1)

**`POST /api/v1/seller/shops`**

| Header | Value | Required |
|--------|-------|----------|
| `Authorization` | `Bearer <access_token>` | Yes |
| `Content-Type` | `application/json` | Yes |

#### Request Body

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `name` | string | Yes | Name of the shop |
| `categoryId` | string | Yes | UUID of the shop category |

#### Request

```bash
curl -X 'POST' \
  'http://localhost:8080/api/v1/seller/shops' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9...' \
  -H 'Content-Type: application/json' \
  -d '{
    "name": "Royal Shop",
    "categoryId": "5e67ec97-9ed6-48ee-9d56-4163fe1711cb"
  }'
```

#### Response

```json
{
    "id": "cbfdcfa3-fb65-4fa3-9078-e0f8cc63ddbc",
    "name": "Royal Shop"
}
```

---

### 2. Upload Shop Logo

**`POST /api/v1/seller/shops/{id}/image`**

#### Multipart Form Data

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `image` | file | Yes | `image/jpeg`, `image/png` or `image/webp` (`.jpg`, `.jpeg`, `.png`, `.webp`). Maximum **5 MB**. |

#### Headers

| Header | Value | Required |
|--------|-------|----------|
| `Authorization` | `Bearer <seller_token>` | Yes |
| `Content-Type` | `multipart/form-data` | Yes |

#### Request

```bash
curl -X 'POST' \
  'http://localhost:8080/api/v1/seller/shops/{id}/image' \
  -H 'Authorization: Bearer <seller_token>' \
  -H 'Content-Type: multipart/form-data' \
  -F 'image=@shop-logo.png;type=image/png'
```

#### Response

```json
{
  "id": "a33b8912-e0b2-4058-9d7b-3c7ef9b935c7",
  "name": "Updated Shop",
  "logo": "/uploads/shop-images/2f8a1c.png"
}
```

---

### 3. Get Public Shops (V1)

**`GET /api/v1/shops/public`**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `status` | string | No | Shop status to filter by (APPROVED, etc.) |
| `category` | string | No | UUID of the category to filter by |
| `limit` | number | No | Maximum number of shops to return |
| `offset` | number | No | Number of shops to skip |

#### Request

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/shops/public?status=APPROVED&limit=10' \
  -H 'accept: application/json'
```

#### Response

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
    "offset": 0
  }
}
```

---

### 4. Admin Shop Approval

**`PUT /api/v1/admin/shops/approve/{id}`**

```bash
curl -X 'PUT' \
  'http://localhost:8080/api/v1/admin/shops/approve/a33b8912-e0b2-4058-9d7b-3c7ef9b935c7' \
  -H 'Authorization: Bearer <admin_token>'
```

#### Response

```json
{
    "id": "a33b8912-e0b2-4058-9d7b-3c7ef9b935c7",
    "status": "APPROVED"
}
```

---

### 5. Seller - Retrieve owned shops

**`GET /api/v1/seller/shops`**

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/seller/shops?page=1&perPage=20' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <seller_token>'
```

#### Response

```json
{
  "data": [
    {
      "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "name": "Example",
      "categoryId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "description": "Looks good",
      "address": "123 Main St",
      "phone": "+15551234567",
      "email": "jane@example.com",
      "logo": "https://example.com/image.jpg",
      "coverImage": "https://example.com/image.jpg",
      "status": "PENDING",
      "rejectionReason": "Policy violation",
      "suspendedReason": "Policy violation",
      "ownerUserId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "ownerEmail": "jane@example.com",
      "kycStatus": "NOT_SUBMITTED",
      "rating": "5",
      "totalReviews": 1,
      "createdAt": "2024-05-06T12:00:00",
      "updatedAt": "2024-05-06T12:00:00"
    }
  ],
  "metadata": {
    "totalCount": 1,
    "limit": 20,
    "offset": 0,
    "page": 1,
    "perPage": 20,
    "totalPages": 1,
    "hasNext": false,
    "hasPrev": false
  }
}
```

---

### 6. Seller - Update shop details

**`PUT /api/v1/seller/shops/{id}`**

```bash
curl -X 'PUT' \
  'http://localhost:8080/api/v1/seller/shops/6c5078d3-f8e3-4c88-9afe-48b5423c664f' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <seller_token>' \
  -H 'Content-Type: application/json' \
  -d '{
  "name": "Example",
  "description": "Looks good",
  "address": "123 Main St",
  "phone": "+15551234567",
  "email": "jane@example.com",
  "logo": "https://example.com/image.jpg",
  "coverImage": "https://example.com/image.jpg"
}'
```

#### Response

```json
{
  "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "name": "Example",
  "categoryId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "description": "Looks good",
  "address": "123 Main St",
  "phone": "+15551234567",
  "email": "jane@example.com",
  "logo": "https://example.com/image.jpg",
  "coverImage": "https://example.com/image.jpg",
  "status": "PENDING",
  "rejectionReason": "Policy violation",
  "suspendedReason": "Policy violation",
  "ownerUserId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "ownerEmail": "jane@example.com",
  "kycStatus": "NOT_SUBMITTED",
  "rating": "5",
  "totalReviews": 1,
  "createdAt": "2024-05-06T12:00:00",
  "updatedAt": "2024-05-06T12:00:00"
}
```

---

### 7. Retrieve detailed information about a specific shop

**`GET /api/v1/shops/{id}`**

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/shops/6c5078d3-f8e3-4c88-9afe-48b5423c664f' \
  -H 'accept: application/json'
```

#### Response

```json
{
  "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "name": "Example",
  "categoryId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "description": "Looks good",
  "address": "123 Main St",
  "phone": "+15551234567",
  "email": "jane@example.com",
  "logo": "https://example.com/image.jpg",
  "coverImage": "https://example.com/image.jpg",
  "status": "PENDING",
  "rejectionReason": "Policy violation",
  "suspendedReason": "Policy violation",
  "ownerUserId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "ownerEmail": "jane@example.com",
  "kycStatus": "NOT_SUBMITTED",
  "rating": "5",
  "totalReviews": 1,
  "createdAt": "2024-05-06T12:00:00",
  "updatedAt": "2024-05-06T12:00:00"
}
```

---

### 8. Retrieve shops by category (public)

**`GET /api/v1/shops/category/{categoryId}`**

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/shops/category/6c5078d3-f8e3-4c88-9afe-48b5423c664f?page=1&perPage=20' \
  -H 'accept: application/json'
```

#### Response

```json
{
  "data": [
    {
      "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "name": "Example",
      "categoryId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "description": "Looks good",
      "address": "123 Main St",
      "phone": "+15551234567",
      "email": "jane@example.com",
      "logo": "https://example.com/image.jpg",
      "coverImage": "https://example.com/image.jpg",
      "status": "PENDING",
      "rejectionReason": "Policy violation",
      "suspendedReason": "Policy violation",
      "ownerUserId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "ownerEmail": "jane@example.com",
      "kycStatus": "NOT_SUBMITTED",
      "rating": "5",
      "totalReviews": 1,
      "createdAt": "2024-05-06T12:00:00",
      "updatedAt": "2024-05-06T12:00:00"
    }
  ],
  "metadata": {
    "totalCount": 1,
    "limit": 20,
    "offset": 0,
    "page": 1,
    "perPage": 20,
    "totalPages": 1,
    "hasNext": false,
    "hasPrev": false
  }
}
```

---

### 9. Retrieve featured shops (public)

**`GET /api/v1/shops/featured`**

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/shops/featured?page=1&perPage=20' \
  -H 'accept: application/json'
```

#### Response

```json
{
  "data": [
    {
      "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "name": "Example",
      "categoryId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "description": "Looks good",
      "address": "123 Main St",
      "phone": "+15551234567",
      "email": "jane@example.com",
      "logo": "https://example.com/image.jpg",
      "coverImage": "https://example.com/image.jpg",
      "status": "PENDING",
      "rejectionReason": "Policy violation",
      "suspendedReason": "Policy violation",
      "ownerUserId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "ownerEmail": "jane@example.com",
      "kycStatus": "NOT_SUBMITTED",
      "rating": "5",
      "totalReviews": 1,
      "createdAt": "2024-05-06T12:00:00",
      "updatedAt": "2024-05-06T12:00:00"
    }
  ],
  "metadata": {
    "totalCount": 1,
    "limit": 20,
    "offset": 0,
    "page": 1,
    "perPage": 20,
    "totalPages": 1,
    "hasNext": false,
    "hasPrev": false
  }
}
```

---

### 10. Admin - Retrieve shops filtered by status

**`GET /api/v1/admin/shops/status`**

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/admin/shops/status?page=1&perPage=20&status=PENDING' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <admin_token>'
```

#### Response

```json
{
  "data": [
    {
      "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "name": "Example",
      "categoryId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "description": "Looks good",
      "address": "123 Main St",
      "phone": "+15551234567",
      "email": "jane@example.com",
      "logo": "https://example.com/image.jpg",
      "coverImage": "https://example.com/image.jpg",
      "status": "PENDING",
      "rejectionReason": "Policy violation",
      "suspendedReason": "Policy violation",
      "ownerUserId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "ownerEmail": "jane@example.com",
      "kycStatus": "NOT_SUBMITTED",
      "rating": "5",
      "totalReviews": 1,
      "createdAt": "2024-05-06T12:00:00",
      "updatedAt": "2024-05-06T12:00:00"
    }
  ],
  "metadata": {
    "totalCount": 1,
    "limit": 20,
    "offset": 0,
    "page": 1,
    "perPage": 20,
    "totalPages": 1,
    "hasNext": false,
    "hasPrev": false
  }
}
```

---

### 11. Admin - Reject a shop application

**`PUT /api/v1/admin/shops/reject/{id}`**

```bash
curl -X 'PUT' \
  'http://localhost:8080/api/v1/admin/shops/reject/6c5078d3-f8e3-4c88-9afe-48b5423c664f' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <admin_token>' \
  -H 'Content-Type: application/json' \
  -d '{
  "reason": "Policy violation"
}'
```

#### Response

```json
{
  "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "name": "Example",
  "categoryId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "description": "Looks good",
  "address": "123 Main St",
  "phone": "+15551234567",
  "email": "jane@example.com",
  "logo": "https://example.com/image.jpg",
  "coverImage": "https://example.com/image.jpg",
  "status": "PENDING",
  "rejectionReason": "Policy violation",
  "suspendedReason": "Policy violation",
  "ownerUserId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "ownerEmail": "jane@example.com",
  "kycStatus": "NOT_SUBMITTED",
  "rating": "5",
  "totalReviews": 1,
  "createdAt": "2024-05-06T12:00:00",
  "updatedAt": "2024-05-06T12:00:00"
}
```

---

### 12. Admin - Suspend an active shop

**`PUT /api/v1/admin/shops/suspend/{id}`**

```bash
curl -X 'PUT' \
  'http://localhost:8080/api/v1/admin/shops/suspend/6c5078d3-f8e3-4c88-9afe-48b5423c664f' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <admin_token>' \
  -H 'Content-Type: application/json' \
  -d '{
  "reason": "Policy violation"
}'
```

#### Response

```json
{
  "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "name": "Example",
  "categoryId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "description": "Looks good",
  "address": "123 Main St",
  "phone": "+15551234567",
  "email": "jane@example.com",
  "logo": "https://example.com/image.jpg",
  "coverImage": "https://example.com/image.jpg",
  "status": "PENDING",
  "rejectionReason": "Policy violation",
  "suspendedReason": "Policy violation",
  "ownerUserId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "ownerEmail": "jane@example.com",
  "kycStatus": "NOT_SUBMITTED",
  "rating": "5",
  "totalReviews": 1,
  "createdAt": "2024-05-06T12:00:00",
  "updatedAt": "2024-05-06T12:00:00"
}
```

---

### 13. Admin - Activate a suspended shop

**`PUT /api/v1/admin/shops/activate/{id}`**

```bash
curl -X 'PUT' \
  'http://localhost:8080/api/v1/admin/shops/activate/6c5078d3-f8e3-4c88-9afe-48b5423c664f' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <admin_token>'
```

#### Response

```json
{
  "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "name": "Example",
  "categoryId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "description": "Looks good",
  "address": "123 Main St",
  "phone": "+15551234567",
  "email": "jane@example.com",
  "logo": "https://example.com/image.jpg",
  "coverImage": "https://example.com/image.jpg",
  "status": "PENDING",
  "rejectionReason": "Policy violation",
  "suspendedReason": "Policy violation",
  "ownerUserId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "ownerEmail": "jane@example.com",
  "kycStatus": "NOT_SUBMITTED",
  "rating": "5",
  "totalReviews": 1,
  "createdAt": "2024-05-06T12:00:00",
  "updatedAt": "2024-05-06T12:00:00"
}
```

---

### 14. Admin - List all shops

**`GET /api/v1/admin/shops`**

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/admin/shops?page=1&perPage=20&status=PENDING&q=phone' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <admin_token>'
```

#### Response

```json
{
  "data": [
    {
      "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "name": "Example",
      "categoryId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "description": "Looks good",
      "address": "123 Main St",
      "phone": "+15551234567",
      "email": "jane@example.com",
      "logo": "https://example.com/image.jpg",
      "coverImage": "https://example.com/image.jpg",
      "status": "PENDING",
      "rejectionReason": "Policy violation",
      "suspendedReason": "Policy violation",
      "ownerUserId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "ownerEmail": "jane@example.com",
      "kycStatus": "NOT_SUBMITTED",
      "rating": "5",
      "totalReviews": 1,
      "createdAt": "2024-05-06T12:00:00",
      "updatedAt": "2024-05-06T12:00:00"
    }
  ],
  "metadata": {
    "totalCount": 1,
    "limit": 20,
    "offset": 0,
    "page": 1,
    "perPage": 20,
    "totalPages": 1,
    "hasNext": false,
    "hasPrev": false
  }
}
```

---

### 15. Admin - Bulk approve shops

**`POST /api/v1/admin/shops/bulk/approve`**

```bash
curl -X 'POST' \
  'http://localhost:8080/api/v1/admin/shops/bulk/approve' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <admin_token>' \
  -H 'Content-Type: application/json' \
  -d '{
  "ids": [
    "6c5078d3-f8e3-4c88-9afe-48b5423c664f"
  ],
  "reason": "Policy violation"
}'
```

#### Response

```json
{
  "requested": 1,
  "succeeded": 1,
  "failed": []
}
```

---

### 16. Admin - Bulk reject shops

**`POST /api/v1/admin/shops/bulk/reject`**

```bash
curl -X 'POST' \
  'http://localhost:8080/api/v1/admin/shops/bulk/reject' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <admin_token>' \
  -H 'Content-Type: application/json' \
  -d '{
  "ids": [
    "6c5078d3-f8e3-4c88-9afe-48b5423c664f"
  ],
  "reason": "Policy violation"
}'
```

#### Response

```json
{
  "requested": 1,
  "succeeded": 1,
  "failed": []
}
```

---

### 17. Admin - Bulk suspend shops

**`POST /api/v1/admin/shops/bulk/suspend`**

```bash
curl -X 'POST' \
  'http://localhost:8080/api/v1/admin/shops/bulk/suspend' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <admin_token>' \
  -H 'Content-Type: application/json' \
  -d '{
  "ids": [
    "6c5078d3-f8e3-4c88-9afe-48b5423c664f"
  ],
  "reason": "Policy violation"
}'
```

#### Response

```json
{
  "requested": 1,
  "succeeded": 1,
  "failed": []
}
```

---

### 18. List products for a shop

**`GET /api/v1/shops/{id}/products`**

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/shops/6c5078d3-f8e3-4c88-9afe-48b5423c664f/products?page=1&perPage=20' \
  -H 'accept: application/json'
```

#### Response

```json
{
  "data": [
    {
      "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "userId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "shopId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "categoryId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "subCategoryId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "brandId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "name": "Example",
      "description": "Looks good",
      "sku": "SKU-12345",
      "barcode": "SAVE10",
      "weight": 250.0,
      "dimensions": "10x20x30 cm",
      "minOrderQuantity": 1,
      "price": "250.00",
      "discountPrice": "250.00",
      "discountPercentage": 250.0,
      "videoLink": "https://example.com/video.mp4",
      "hotDeal": true,
      "featured": true,
      "bestSeller": true
    }
  ],
  "metadata": {
    "totalCount": 1,
    "limit": 20,
    "offset": 0,
    "page": 1,
    "perPage": 20,
    "totalPages": 1,
    "hasNext": false,
    "hasPrev": false
  }
}
```

---

### 19. Seller: list shop members

**`GET /api/v1/seller/members`**

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/seller/members' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <seller_token>'
```

#### Response

```json
[
  {
    "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
    "shopId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
    "userId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
    "email": "jane@example.com",
    "status": "PENDING",
    "isOwner": true,
    "roles": [
      "string"
    ]
  }
]
```

---

### 20. Seller: invite a staff member

**`POST /api/v1/seller/members/invite`**

```bash
curl -X 'POST' \
  'http://localhost:8080/api/v1/seller/members/invite' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <seller_token>' \
  -H 'Content-Type: application/json' \
  -d '{
  "email": "jane@example.com",
  "roleIds": [
    "string"
  ]
}'
```

#### Response

```json
{
  "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "shopId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "email": "jane@example.com",
  "expiresAt": "2024-05-06T12:00:00",
  "token": "token-value"
}
```

---

### 21. Seller: replace a member's roles

**`PUT /api/v1/seller/members/{id}/roles`**

```bash
curl -X 'PUT' \
  'http://localhost:8080/api/v1/seller/members/6c5078d3-f8e3-4c88-9afe-48b5423c664f/roles' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <seller_token>' \
  -H 'Content-Type: application/json' \
  -d '{
  "roleIds": [
    "string"
  ]
}'
```

#### Response

```json
{
  "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "shopId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "userId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "email": "jane@example.com",
  "status": "PENDING",
  "isOwner": true,
  "roles": [
    "string"
  ]
}
```

---

### 22. Seller: remove a shop member

**`DELETE /api/v1/seller/members/{id}`**

```bash
curl -X 'DELETE' \
  'http://localhost:8080/api/v1/seller/members/6c5078d3-f8e3-4c88-9afe-48b5423c664f' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <seller_token>'
```

#### Response

```json
{
  "message": "Operation completed"
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
| `500` | Internal Server Error |
