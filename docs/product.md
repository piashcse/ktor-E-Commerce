# Product API

This documentation provides comprehensive details for the Product API endpoints. The API supports managing products within the platform, including creating, retrieving, updating, and deleting products. Products are associated with categories and can be managed by authorized sellers or administrators.

**Base URL:** `http://localhost:8080`

## Authentication

Most Product API endpoints require Bearer token authentication. Include the access token in the Authorization header:

```
Authorization: Bearer <your_access_token>
```

### Product Endpoints

| Method | Endpoint | Description | Authentication Required |
|--------|----------|-------------|------------------------|
| `POST` | `/api/v1/seller/products` | Create a new product (Seller) | Yes (Seller) |
| `PUT` | `/api/v1/seller/products/{id}` | Update an existing product (Seller) | Yes (Seller) |
| `DELETE` | `/api/v1/seller/products/{id}` | Delete a product (Seller) | Yes (Seller) |
| `GET` | `/api/v1/seller/products` | Retrieve products belonging to seller | Yes (Seller) |
| `POST` | `/api/v1/seller/products/image-upload` | Upload product image (Seller) | Yes (Seller) |
| `GET` | `/api/v1/products/{id}` | Retrieve a specific product | No |
| `GET` | `/api/v1/products/search` | Search products with filters | No |
| `DELETE` | `/api/v1/admin/products/{id}` | Delete a product (Admin) | Yes (Admin) |

---

## Endpoint Details

### 1. Create Product (Seller)

**`POST /api/v1/seller/products`**

Create a new product as a seller. The product will be automatically associated with your shop.

#### Example Request

```bash
curl -X 'POST' \
  'http://localhost:8080/api/v1/seller/products' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <seller_token>' \
  -H 'Content-Type: application/json' \
  -d '{
  "categoryId": "b4f08aae-b1af-4617-963a-b0b9d1187646",
  "name": "Smart watch",
  "description": "Premium smart watch",
  "stockQuantity": 50,
  "price": 199.99,
  "hotDeal": true,
  "featured": false,
  "images": ["image1.png"]
}'
```

#### Example Response

```json
{
  "id": "718f0b9a-24ef-450f-9126-7d3d9b27cad5",
  "name": "Smart watch",
  "status": "ACTIVE"
}
```

---

### 2. Search Products

**`GET /api/v1/products/search`**

Search for products using various filters. This is a public endpoint.

#### Query Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `categoryId` | string | No | Filter by category |
| `subCategoryId` | string | No | Filter by sub-category |
| `brandId` | string | No | Filter by brand |
| `minPrice` | number | No | Minimum price |
| `maxPrice` | number | No | Maximum price |
| `sortBy` | string | No | price, name, createdAt |
| `sortOrder` | string | No | asc, desc (Default: desc) |
| `limit` | number | No | Default: 20 |
| `offset` | number | No | Default: 0 |

#### Example Request

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/products?limit=10&sortBy=price&sortOrder=asc' \
  -H 'accept: application/json'
```

---

### 3. Get Seller Products

**`GET /api/v1/seller/products`**

Retrieve all products belonging to the authenticated seller.

#### Example Request

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/seller/products?limit=10' \
  -H 'Authorization: Bearer <seller_token>'
```

---

### 4. Upload Product Image

**`POST /api/v1/seller/products/image-upload`**

Upload an image for a specific product.

#### Example Request

```bash
curl -X 'POST' \
  'http://localhost:8080/api/v1/seller/products/image-upload?id=718f0b9a-24ef-450f-9126-7d3d9b27cad5' \
  -H 'Authorization: Bearer <seller_token>' \
  -H 'Content-Type: multipart/form-data' \
  -F 'file=@product_watch.png;type=image/png'
```

---

## Error Handling

### Success Responses
- **HTTP 200/201**: Success.

### Common Error Codes
| Status Code | Description |
|-------------|-------------|
| `400` | Bad Request (Validation error) |
| `401` | Unauthorized (Invalid token) |
| `403` | Forbidden (User is not the owner) |
| `404` | Not Found |
