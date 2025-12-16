# Product API

This documentation provides comprehensive details for the Product API endpoints. The API supports managing products within the platform, including creating, retrieving, updating, and deleting products. Products are associated with categories and can be managed by authorized sellers.

**Base URL:** `http://localhost:8080`

## Authentication

All Product API endpoints require Bearer token authentication. Include the access token in the Authorization header:

```
Authorization: Bearer <your_access_token>
```

### Product Endpoints

| Method | Endpoint | Description | Authentication Required |
|--------|----------|-------------|------------------------|
| `POST` | `/product` | Create a new product | Yes |
| `GET` | `/product/{id}` | Retrieve a specific product | Yes |
| `PUT` | `/product/{id}` | Update an existing product | Yes |
| `GET` | `/product` | Retrieve list of products with filters | Yes |
| `GET` | `/product/search` | Search products with filters | No |
| `GET` | `/product/seller` | Retrieve products belonging to seller | Yes |
| `GET` | `/product/featured` | Retrieve featured products | No |
| `GET` | `/product/best-selling` | Retrieve best selling products | No |
| `GET` | `/product/hot-deals` | Retrieve products on hot deals | No |
| `DELETE` | `/product/{id}` | Delete a product | Yes |
| `POST` | `/product/image-upload` | Upload product image | Yes |

---

## Product API Details

### 1. Create Product

**`POST /product`**

Create a new product with specified details including name, description, price, and category information.

#### Request Body

| Field | Type | Required | Validation | Description |
|-------|------|----------|------------|-------------|
| `categoryId` | string | Yes | Not null, not empty | UUID of the product category |
| `subCategoryId` | string | No | - | UUID of the product subcategory |
| `brandId` | string | No | - | UUID of the product brand |
| `name` | string | Yes | Not null, not empty | Product name |
| `description` | string | Yes | Not null, not empty | Product description |
| `productCode` | string | No | - | Unique product code |
| `stockQuantity` | integer | Yes | Not null, > 0 | Available stock quantity |
| `price` | number | Yes | Not null, > 0.0 | Product price |
| `discountPrice` | number | No | - | Discounted price (optional) |
| `status` | integer | No | - | Product status code |
| `videoLink` | string | No | - | URL to product video |
| `hotDeal` | boolean | Yes | - | Whether product is a hot deal |
| `featured` | boolean | Yes | - | Whether product is featured |
| `images` | array | Yes | - | Array of image URLs |

```json
{
  "categoryId": "string",
  "subCategoryId": "string|null",
  "brandId": "string|null",
  "name": "string",
  "description": "string",
  "productCode": "string|null",
  "stockQuantity": 1,
  "price": 100.0,
  "discountPrice": 80.0,
  "status": 1,
  "videoLink": "string|null",
  "hotDeal": true,
  "featured": true,
  "images": ["string"]
}
```

#### Example Request

```bash
curl -X 'POST' \
  'http://localhost:8080/product' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <your_token>' \
  -H 'Content-Type: application/json' \
  -d '{
  "categoryId": "b4f08aae-b1af-4617-963a-b0b9d1187646",
  "subCategoryId": null,
  "brandId": null,
  "name": "Smart watch",
  "description": "Good watch",
  "productCode": null,
  "stockQuantity": 1,
  "price": 100.0,
  "discountPrice": null,
  "status": null,
  "videoLink": null,
  "hotDeal": true,
  "featured": true,
  "images": ["string"]
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
    "id": "718f0b9a-24ef-450f-9126-7d3d9b27cad5",
    "categoryId": "b4f08aae-b1af-4617-963a-b0b9d1187646",
    "name": "Smart watch",
    "description": "Good watch",
    "minOrderQuantity": 1,
    "stockQuantity": 1,
    "price": 100,
    "hotDeal": true,
    "featured": true,
    "images": "[string]",
    "status": "ACTIVE"
  }
}
```

---

### 2. Get Product by ID

**`GET /product/{id}`**

Retrieve a specific product by its unique identifier.

#### Path Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `id` | string | Yes | Unique identifier of the product |

#### Example Request

```bash
curl -X 'GET' \
  'http://localhost:8080/product/79a97389-78d5-4dff-a1f7-13bc7ae10a8d' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <your_token>'
```

---

### 3. Update Product

**`PUT /product/{id}`**

Update an existing product's information.

#### Path Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `id` | string | Yes | Unique identifier of the product to update |

#### Request Body

```json
{
  "name": "string",
  "detail": "string"
}
```

#### Example Request

```bash
curl -X 'PUT' \
  'http://localhost:8080/product/718f0b9a-24ef-450f-9126-7d3d9b27cad5' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <your_token>' \
  -H 'Content-Type: application/json' \
  -d '{
  "name": "Smart watch",
  "detail": "Xiaomi Smart Watch"
}'
```

---

### 4. Get Products with Filters

**`GET /product`**

Retrieve a list of products with optional filtering and pagination.

#### Query Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `limit` | number | No | Maximum number of products to return |
| `maxPrice` | number | No | Maximum price filter |
| `minPrice` | number | No | Minimum price filter |

#### Example Request

```bash
curl -X 'GET' \
  'http://localhost:8080/product?limit=10&maxPrice=100&minPrice=0' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <your_token>'
```

---

### 5. Delete Product

**`DELETE /product/{id}`**

Delete a product by its ID.

#### Query Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `productId` | string | Yes | Unique identifier of the product to delete |

#### Example Request

```bash
curl -X 'DELETE' \
  'http://localhost:8080/product?productId=79a97389-78d5-4dff-a1f7-13bc7ae10a8d' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <your_token>'
```

---

### 6. Upload Product Image

**`POST /product/image-upload`**

Upload an image for a specific product.

#### Query Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `id` | string | Yes | Product ID to associate the image with |

#### Request Body

Form data with file upload:
- `file`: Image file (PNG, JPG, etc.)

#### Example Request

```bash
curl -X 'POST' \
  'http://localhost:8080/product/image-upload?id=71b26dd9-b4b5-4f87-a84d-c8daa506018a' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <your_token>' \
  -H 'Content-Type: multipart/form-data' \
  -F 'file=@image.png;type=image/png'
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
    "id": "cc38e31e-3a7f-435c-9e86-293daf0d6877",
    "imageUrl": "bf68a3f9-d131-4bee-bbbc-80264a3da437.png"
  }
}
```

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
| `404` | Not Found - Product or category not found |
| `409` | Conflict - Product code already exists or invalid category |
| `500` | Internal Server Error - Server error |
