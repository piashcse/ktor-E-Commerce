# Coupon Management

Fixed and percentage discounts, coupon validation and automatic promotions.

## Endpoints

| Method | Endpoint | Description | Authentication Required |
|--------|----------|-------------|------------------------|
| `GET` | `/api/v1/admin/coupons` | Admin - Retrieve a list of all coupons | Yes (Admin) |
| `POST` | `/api/v1/admin/coupons` | Admin - Create a new discount coupon | Yes (Admin) |
| `DELETE` | `/api/v1/admin/coupons/{id}` | Admin - Delete a coupon | Yes (Admin) |
| `PUT` | `/api/v1/admin/coupons/{id}` | Admin - Update an existing coupon | Yes (Admin) |
| `GET` | `/api/v1/admin/promotions` | Admin: list promotions | Yes (Admin) |
| `POST` | `/api/v1/admin/promotions` | Admin: create a promotion | Yes (Admin) |
| `DELETE` | `/api/v1/admin/promotions/{id}` | Admin: deactivate a promotion | Yes (Admin) |
| `PUT` | `/api/v1/admin/promotions/{id}` | Admin: update a promotion | Yes (Admin) |
| `GET` | `/api/v1/coupons/{code}` | Retrieve detailed information about a coupon by its code | No |
| `GET` | `/api/v1/seller/coupons` | Seller - List my shop coupons | Yes (Seller) |
| `POST` | `/api/v1/seller/coupons` | Seller - Create a shop coupon | Yes (Seller) |
| `DELETE` | `/api/v1/seller/coupons/{id}` | Seller - Delete a shop coupon | Yes (Seller) |
| `PUT` | `/api/v1/seller/coupons/{id}` | Seller - Update a shop coupon | Yes (Seller) |
| `GET` | `/api/v1/seller/promotions` | Seller: list my promotions | Yes (Seller) |
| `POST` | `/api/v1/seller/promotions` | Seller: create a promotion | Yes (Seller) |
| `DELETE` | `/api/v1/seller/promotions/{id}` | Seller: deactivate a promotion | Yes (Seller) |
| `PUT` | `/api/v1/seller/promotions/{id}` | Seller: update a promotion | Yes (Seller) |

## Endpoint Details

### 1. Admin - Retrieve a list of all coupons

**`GET /api/v1/admin/coupons`**

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/admin/coupons?page=1&perPage=20&active=true' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <admin_token>'
```

#### Response

```json
{
  "data": [
    {
      "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "code": "SAVE10",
      "discountType": "FIXED",
      "discountValue": "250.00",
      "minOrderAmount": "250.00",
      "maxDiscountAmount": "250.00",
      "startDate": "2024-05-06T12:00:00",
      "endDate": "2024-05-06T12:00:00",
      "usageLimit": 1,
      "usageCount": 1,
      "perUserLimit": 1,
      "shopId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "categoryId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "isActive": true
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

### 2. Admin - Create a new discount coupon

**`POST /api/v1/admin/coupons`**

```bash
curl -X 'POST' \
  'http://localhost:8080/api/v1/admin/coupons' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <admin_token>' \
  -H 'Content-Type: application/json' \
  -d '{
  "code": "SAVE10",
  "discountType": "FIXED",
  "discountValue": "250.00",
  "minOrderAmount": "250.00",
  "maxDiscountAmount": "250.00",
  "startDate": "2024-05-06T12:00:00",
  "endDate": "2024-05-06T12:00:00",
  "usageLimit": 1,
  "perUserLimit": 1,
  "shopId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "categoryId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "isActive": true
}'
```

#### Response

```json
{
  "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "code": "SAVE10",
  "discountType": "FIXED",
  "discountValue": "250.00",
  "minOrderAmount": "250.00",
  "maxDiscountAmount": "250.00",
  "startDate": "2024-05-06T12:00:00",
  "endDate": "2024-05-06T12:00:00",
  "usageLimit": 1,
  "usageCount": 1,
  "perUserLimit": 1,
  "shopId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "categoryId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "isActive": true
}
```

---

### 3. Admin - Delete a coupon

**`DELETE /api/v1/admin/coupons/{id}`**

```bash
curl -X 'DELETE' \
  'http://localhost:8080/api/v1/admin/coupons/6c5078d3-f8e3-4c88-9afe-48b5423c664f' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <admin_token>'
```

#### Response

```json
{
  "message": "Operation completed"
}
```

---

### 4. Admin - Update an existing coupon

**`PUT /api/v1/admin/coupons/{id}`**

```bash
curl -X 'PUT' \
  'http://localhost:8080/api/v1/admin/coupons/6c5078d3-f8e3-4c88-9afe-48b5423c664f' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <admin_token>' \
  -H 'Content-Type: application/json' \
  -d '{
  "code": "SAVE10",
  "discountType": "FIXED",
  "discountValue": "250.00",
  "minOrderAmount": "250.00",
  "maxDiscountAmount": "250.00",
  "startDate": "2024-05-06T12:00:00",
  "endDate": "2024-05-06T12:00:00",
  "usageLimit": 1,
  "perUserLimit": 1,
  "shopId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "categoryId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "isActive": true
}'
```

#### Response

```json
{
  "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "code": "SAVE10",
  "discountType": "FIXED",
  "discountValue": "250.00",
  "minOrderAmount": "250.00",
  "maxDiscountAmount": "250.00",
  "startDate": "2024-05-06T12:00:00",
  "endDate": "2024-05-06T12:00:00",
  "usageLimit": 1,
  "usageCount": 1,
  "perUserLimit": 1,
  "shopId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "categoryId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "isActive": true
}
```

---

### 5. Admin: list promotions

**`GET /api/v1/admin/promotions`**

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/admin/promotions?page=1&perPage=20&active=true' \
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
      "type": "PERCENTAGE",
      "value": "10.00",
      "minOrderAmount": "250.00",
      "maxDiscountAmount": "250.00",
      "startsAt": "2024-05-06T12:00:00",
      "endsAt": "2024-05-06T12:00:00",
      "isActive": true,
      "priority": 1,
      "usageLimit": 1,
      "usageCount": 1,
      "shopId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "createdAt": "2024-05-06T12:00:00"
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

### 6. Admin: create a promotion

**`POST /api/v1/admin/promotions`**

```bash
curl -X 'POST' \
  'http://localhost:8080/api/v1/admin/promotions' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <admin_token>' \
  -H 'Content-Type: application/json' \
  -d '{
  "name": "Example",
  "type": "PERCENTAGE",
  "value": "10.00",
  "minOrderAmount": "250.00",
  "maxDiscountAmount": "250.00",
  "startsAt": "2024-05-06T12:00:00",
  "endsAt": "2024-05-06T12:00:00",
  "isActive": true,
  "priority": 1,
  "usageLimit": 1,
  "shopId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f"
}'
```

#### Response

```json
{
  "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "name": "Example",
  "type": "PERCENTAGE",
  "value": "10.00",
  "minOrderAmount": "250.00",
  "maxDiscountAmount": "250.00",
  "startsAt": "2024-05-06T12:00:00",
  "endsAt": "2024-05-06T12:00:00",
  "isActive": true,
  "priority": 1,
  "usageLimit": 1,
  "usageCount": 1,
  "shopId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "createdAt": "2024-05-06T12:00:00"
}
```

---

### 7. Admin: deactivate a promotion

**`DELETE /api/v1/admin/promotions/{id}`**

```bash
curl -X 'DELETE' \
  'http://localhost:8080/api/v1/admin/promotions/6c5078d3-f8e3-4c88-9afe-48b5423c664f' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <admin_token>'
```

#### Response

```json
{
  "message": "Operation completed"
}
```

---

### 8. Admin: update a promotion

**`PUT /api/v1/admin/promotions/{id}`**

```bash
curl -X 'PUT' \
  'http://localhost:8080/api/v1/admin/promotions/6c5078d3-f8e3-4c88-9afe-48b5423c664f' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <admin_token>' \
  -H 'Content-Type: application/json' \
  -d '{
  "name": "Example",
  "type": "PERCENTAGE",
  "value": "10.00",
  "minOrderAmount": "250.00",
  "maxDiscountAmount": "250.00",
  "startsAt": "2024-05-06T12:00:00",
  "endsAt": "2024-05-06T12:00:00",
  "isActive": true,
  "priority": 1,
  "usageLimit": 1,
  "shopId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f"
}'
```

#### Response

```json
{
  "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "name": "Example",
  "type": "PERCENTAGE",
  "value": "10.00",
  "minOrderAmount": "250.00",
  "maxDiscountAmount": "250.00",
  "startsAt": "2024-05-06T12:00:00",
  "endsAt": "2024-05-06T12:00:00",
  "isActive": true,
  "priority": 1,
  "usageLimit": 1,
  "usageCount": 1,
  "shopId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "createdAt": "2024-05-06T12:00:00"
}
```

---

### 9. Retrieve detailed information about a coupon by its code

**`GET /api/v1/coupons/{code}`**

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/coupons/SAVE10' \
  -H 'accept: application/json'
```

#### Response

```json
{
  "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "code": "SAVE10",
  "discountType": "FIXED",
  "discountValue": "250.00",
  "minOrderAmount": "250.00",
  "maxDiscountAmount": "250.00",
  "startDate": "2024-05-06T12:00:00",
  "endDate": "2024-05-06T12:00:00",
  "usageLimit": 1,
  "usageCount": 1,
  "perUserLimit": 1,
  "shopId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "categoryId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "isActive": true
}
```

---

### 10. Seller - List my shop coupons

**`GET /api/v1/seller/coupons`**

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/seller/coupons?page=1&perPage=20&active=true' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <seller_token>'
```

#### Response

```json
{
  "data": [
    {
      "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "code": "SAVE10",
      "discountType": "FIXED",
      "discountValue": "250.00",
      "minOrderAmount": "250.00",
      "maxDiscountAmount": "250.00",
      "startDate": "2024-05-06T12:00:00",
      "endDate": "2024-05-06T12:00:00",
      "usageLimit": 1,
      "usageCount": 1,
      "perUserLimit": 1,
      "shopId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "categoryId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "isActive": true
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

### 11. Seller - Create a shop coupon

**`POST /api/v1/seller/coupons`**

```bash
curl -X 'POST' \
  'http://localhost:8080/api/v1/seller/coupons' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <seller_token>' \
  -H 'Content-Type: application/json' \
  -d '{
  "code": "SAVE10",
  "discountType": "FIXED",
  "discountValue": "250.00",
  "minOrderAmount": "250.00",
  "maxDiscountAmount": "250.00",
  "startDate": "2024-05-06T12:00:00",
  "endDate": "2024-05-06T12:00:00",
  "usageLimit": 1,
  "perUserLimit": 1,
  "shopId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "categoryId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "isActive": true
}'
```

#### Response

```json
{
  "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "code": "SAVE10",
  "discountType": "FIXED",
  "discountValue": "250.00",
  "minOrderAmount": "250.00",
  "maxDiscountAmount": "250.00",
  "startDate": "2024-05-06T12:00:00",
  "endDate": "2024-05-06T12:00:00",
  "usageLimit": 1,
  "usageCount": 1,
  "perUserLimit": 1,
  "shopId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "categoryId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "isActive": true
}
```

---

### 12. Seller - Delete a shop coupon

**`DELETE /api/v1/seller/coupons/{id}`**

```bash
curl -X 'DELETE' \
  'http://localhost:8080/api/v1/seller/coupons/6c5078d3-f8e3-4c88-9afe-48b5423c664f' \
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

### 13. Seller - Update a shop coupon

**`PUT /api/v1/seller/coupons/{id}`**

```bash
curl -X 'PUT' \
  'http://localhost:8080/api/v1/seller/coupons/6c5078d3-f8e3-4c88-9afe-48b5423c664f' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <seller_token>' \
  -H 'Content-Type: application/json' \
  -d '{
  "code": "SAVE10",
  "discountType": "FIXED",
  "discountValue": "250.00",
  "minOrderAmount": "250.00",
  "maxDiscountAmount": "250.00",
  "startDate": "2024-05-06T12:00:00",
  "endDate": "2024-05-06T12:00:00",
  "usageLimit": 1,
  "perUserLimit": 1,
  "shopId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "categoryId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "isActive": true
}'
```

#### Response

```json
{
  "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "code": "SAVE10",
  "discountType": "FIXED",
  "discountValue": "250.00",
  "minOrderAmount": "250.00",
  "maxDiscountAmount": "250.00",
  "startDate": "2024-05-06T12:00:00",
  "endDate": "2024-05-06T12:00:00",
  "usageLimit": 1,
  "usageCount": 1,
  "perUserLimit": 1,
  "shopId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "categoryId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "isActive": true
}
```

---

### 14. Seller: list my promotions

**`GET /api/v1/seller/promotions`**

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/seller/promotions?page=1&perPage=20&active=true' \
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
      "type": "PERCENTAGE",
      "value": "10.00",
      "minOrderAmount": "250.00",
      "maxDiscountAmount": "250.00",
      "startsAt": "2024-05-06T12:00:00",
      "endsAt": "2024-05-06T12:00:00",
      "isActive": true,
      "priority": 1,
      "usageLimit": 1,
      "usageCount": 1,
      "shopId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "createdAt": "2024-05-06T12:00:00"
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

### 15. Seller: create a promotion

**`POST /api/v1/seller/promotions`**

```bash
curl -X 'POST' \
  'http://localhost:8080/api/v1/seller/promotions' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <seller_token>' \
  -H 'Content-Type: application/json' \
  -d '{
  "name": "Example",
  "type": "PERCENTAGE",
  "value": "10.00",
  "minOrderAmount": "250.00",
  "maxDiscountAmount": "250.00",
  "startsAt": "2024-05-06T12:00:00",
  "endsAt": "2024-05-06T12:00:00",
  "isActive": true,
  "priority": 1,
  "usageLimit": 1,
  "shopId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f"
}'
```

#### Response

```json
{
  "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "name": "Example",
  "type": "PERCENTAGE",
  "value": "10.00",
  "minOrderAmount": "250.00",
  "maxDiscountAmount": "250.00",
  "startsAt": "2024-05-06T12:00:00",
  "endsAt": "2024-05-06T12:00:00",
  "isActive": true,
  "priority": 1,
  "usageLimit": 1,
  "usageCount": 1,
  "shopId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "createdAt": "2024-05-06T12:00:00"
}
```

---

### 16. Seller: deactivate a promotion

**`DELETE /api/v1/seller/promotions/{id}`**

```bash
curl -X 'DELETE' \
  'http://localhost:8080/api/v1/seller/promotions/6c5078d3-f8e3-4c88-9afe-48b5423c664f' \
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

### 17. Seller: update a promotion

**`PUT /api/v1/seller/promotions/{id}`**

```bash
curl -X 'PUT' \
  'http://localhost:8080/api/v1/seller/promotions/6c5078d3-f8e3-4c88-9afe-48b5423c664f' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <seller_token>' \
  -H 'Content-Type: application/json' \
  -d '{
  "name": "Example",
  "type": "PERCENTAGE",
  "value": "10.00",
  "minOrderAmount": "250.00",
  "maxDiscountAmount": "250.00",
  "startsAt": "2024-05-06T12:00:00",
  "endsAt": "2024-05-06T12:00:00",
  "isActive": true,
  "priority": 1,
  "usageLimit": 1,
  "shopId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f"
}'
```

#### Response

```json
{
  "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "name": "Example",
  "type": "PERCENTAGE",
  "value": "10.00",
  "minOrderAmount": "250.00",
  "maxDiscountAmount": "250.00",
  "startsAt": "2024-05-06T12:00:00",
  "endsAt": "2024-05-06T12:00:00",
  "isActive": true,
  "priority": 1,
  "usageLimit": 1,
  "usageCount": 1,
  "shopId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "createdAt": "2024-05-06T12:00:00"
}
```

---

## Automatic promotions

Promotions apply without a code when the order is eligible. If a coupon code is supplied, the coupon is used instead of an automatic promotion.

```json
{
  "name": "Spring 10%",
  "type": "PERCENTAGE",
  "value": 10,
  "minOrderAmount": 50,
  "maxDiscountAmount": 25,
  "startsAt": "2026-03-01T00:00:00",
  "endsAt": "2026-03-31T23:59:59",
  "priority": 10,
  "usageLimit": 1000
}
```

Types: `PERCENTAGE`, `FIXED`, `FREE_SHIPPING`. The highest-priority eligible promotion is used; usage limits are enforced atomically when the order is placed.
