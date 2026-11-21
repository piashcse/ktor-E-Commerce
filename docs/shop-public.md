# Public Shop Discovery

Public shop discovery; no authentication required.

## Endpoints

| Method | Endpoint | Description |
|---|---|---|
| <span class="method-badge get">GET</span> | `/api/v1/shops/{id}` | Retrieve a shop by ID (only `APPROVED` shops are public) |
| <span class="method-badge get">GET</span> | `/api/v1/shops/public` | List public shops with filters and pagination |
| <span class="method-badge get">GET</span> | `/api/v1/shops/category/{categoryId}` | List public shops in a category |
| <span class="method-badge get">GET</span> | `/api/v1/shops/featured` | List featured shops |

## Filters

`GET /api/v1/shops/public` accepts:

| Parameter | Type | Description |
|---|---|---|
| `search` | string | Match shop name/description |
| `categoryId` | string | Filter by shop category |
| `limit` / `offset` | number | Offset pagination (or `page`/`perPage`) |

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/shops/public?categoryId=9c95c44c-3767-4ca2-9486-e28e390b3741&page=1&perPage=20'
```

Only shops with `status = APPROVED` are returned. Pending/suspended shops return `404` from the
public detail endpoint and are excluded from all public lists.
## Endpoint Details

### 1. Retrieve detailed information about a specific shop

<div class="endpoint-header">
  <span class="method-badge get">GET</span>
  <span class="endpoint-path">/api/v1/shops/{id}</span>
</div>

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

### 2. Retrieve public shops with filters (public)

<div class="endpoint-header">
  <span class="method-badge get">GET</span>
  <span class="endpoint-path">/api/v1/shops/public</span>
</div>

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/shops/public?page=1&perPage=20&status=PENDING' \
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

### 3. Retrieve shops by category (public)

<div class="endpoint-header">
  <span class="method-badge get">GET</span>
  <span class="endpoint-path">/api/v1/shops/category/{categoryId}</span>
</div>

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

### 4. Retrieve featured shops (public)

<div class="endpoint-header">
  <span class="method-badge get">GET</span>
  <span class="endpoint-path">/api/v1/shops/featured</span>
</div>

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
