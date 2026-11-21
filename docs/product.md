# Product

> Note: `status` is a string enum (`ACTIVE`, `OUT_OF_STOCK`). Optional fields have defaults; partial updates are supported. API

This documentation provides comprehensive details for the Product API endpoints. The API supports managing products within the platform, including creating, retrieving, updating, and deleting products. Products are associated with categories and can be managed by authorized sellers or administrators.

## Authentication

Most Product API endpoints require Bearer token authentication. Include the access token in the Authorization header:

```
Authorization: Bearer <your_access_token>
```

### Product

> Note: `status` is a string enum (`ACTIVE`, `OUT_OF_STOCK`). Optional fields have defaults; partial updates are supported. Endpoints

| Method | Endpoint | Description | Authentication Required |
|--------|----------|-------------|------------------------|
| `POST` | `/api/v1/seller/products` | Create a new product (Seller) | Yes (Seller) |
| `PUT` | `/api/v1/seller/products/{id}` | Update an existing product (Seller) | Yes (Seller) |
| `DELETE` | `/api/v1/seller/products/{id}` | Delete a product (Seller) | Yes (Seller) |
| `GET` | `/api/v1/seller/products` | Retrieve products belonging to seller | Yes (Seller) |
| `POST` | `/api/v1/seller/products/image-upload` | Upload product image (Seller) | Yes (Seller) |
| `GET` | `/api/v1/products` | Retrieve a paginated list of products with optional filters | No |
| `GET` | `/api/v1/products/search` | Search products with filters | No |
| `GET` | `/api/v1/products/suggest` | Autocomplete over products, categories and brands | No |
| `GET` | `/api/v1/products/{id}` | Retrieve a specific product | No |
| `GET` | `/api/v1/products/{id}/variants` | List purchasable variants | No |
| `GET` | `/api/v1/products/{id}/options` | List option dimensions and values | No |
| `GET` | `/api/v1/products/{id}/related` | Related products | No |
| `GET` | `/api/v1/products/{id}/also-bought` | Frequently bought together | No |
| `GET` | `/api/v1/products/recommendations` | Best-seller recommendations | No |
| `DELETE` | `/api/v1/admin/products/{id}` | Delete a product (Admin) | Yes (Admin) |
| `PUT` | `/api/v1/admin/products/{id}/tax-class` | Assign a product tax class | Yes |
| `GET` | `/api/v1/admin/products/{id}/translations` | List a product's translations | Yes |
| `DELETE` | `/api/v1/admin/products/{id}/translations/{locale}` | Delete a product translation | Yes |
| `PUT` | `/api/v1/admin/products/{id}/translations/{locale}` | Create or update a product translation | Yes |
| `POST` | `/api/v1/seller/products/{id}/options` | Create option dimension | Yes |
| `DELETE` | `/api/v1/seller/products/{id}/options/{optionId}` | Delete option dimension | Yes |
| `POST` | `/api/v1/seller/products/{id}/options/{optionId}/values` | Add option value | Yes |
| `DELETE` | `/api/v1/seller/products/{id}/options/{optionId}/values/{valueId}` | Delete option value | Yes |
| `POST` | `/api/v1/seller/products/{id}/variants` | Create product variant | Yes |
| `DELETE` | `/api/v1/seller/products/{id}/variants/{variantId}` | Delete product variant | Yes |
| `PUT` | `/api/v1/seller/products/{id}/variants/{variantId}` | Update product variant | Yes |
| `PUT` | `/api/v1/seller/products/{id}/variants/{variantId}/options` | Attach option values to a variant | Yes |
| `GET` | `/api/v1/stock-alerts` | List back-in-stock alerts | Yes |
| `POST` | `/api/v1/stock-alerts` | Subscribe to a back-in-stock alert | Yes |
| `DELETE` | `/api/v1/stock-alerts/{productId}` | Remove a back-in-stock alert | Yes |

---

## Endpoint Details

### 1. Create Product (Seller)

**`POST /api/v1/seller/products`**

Create a new product as a seller. The product will be automatically associated with your shop.

#### Request

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

#### Response

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

#### Request

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/products?limit=10&sortBy=price&sortOrder=asc' \
  -H 'accept: application/json'
```

---

### 3. Get Seller Products

**`GET /api/v1/seller/products`**

Retrieve all products belonging to the authenticated seller.

#### Request

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/seller/products?limit=10' \
  -H 'Authorization: Bearer <seller_token>'
```

---

### 4. Upload Product Image

**`POST /api/v1/seller/products/image-upload`**

Upload an image for a specific product.

#### Request

```bash
curl -X 'POST' \
  'http://localhost:8080/api/v1/seller/products/image-upload?id=718f0b9a-24ef-450f-9126-7d3d9b27cad5' \
  -H 'Authorization: Bearer <seller_token>' \
  -H 'Content-Type: multipart/form-data' \
  -F 'file=@product_watch.png;type=image/png'
```

---

### 5. Update product

**`PUT /api/v1/seller/products/{id}`**

Update product

#### Request

```bash
curl -X 'PUT' \
  'http://localhost:8080/api/v1/seller/products/6c5078d3-f8e3-4c88-9afe-48b5423c664f' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <seller_token>' \
  -H 'Content-Type: application/json' \
  -d '{
  "categoryId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "subCategoryId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "brandId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "name": "Example",
  "description": "Looks good",
  "productCode": "SAVE10",
  "barcode": "SAVE10",
  "weight": 250.0,
  "dimensions": "10x20x30 cm",
  "minOrderQuantity": 1,
  "price": "250.00",
  "discountPrice": "250.00",
  "clearDiscountPrice": true,
  "status": "ACTIVE",
  "videoLink": "https://example.com/video.mp4",
  "hotDeal": true,
  "featured": true,
  "freeShipping": true,
  "images": [
    "https://example.com/image.jpg"
  ],
  "isDigital": true
}'
```

#### Response

```json
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
```

---

### 6. Delete product

**`DELETE /api/v1/seller/products/{id}`**

Delete product

#### Request

```bash
curl -X 'DELETE' \
  'http://localhost:8080/api/v1/seller/products/6c5078d3-f8e3-4c88-9afe-48b5423c664f' \
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

### 7. Retrieve a paginated list of products with optional filters

**`GET /api/v1/products`**

Retrieve a paginated list of products with optional filters

#### Request

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/products?page=1&perPage=20' \
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

### 8. Product search suggestions

**`GET /api/v1/products/suggest`**

Product search suggestions

#### Request

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/products/suggest?limit=20&q=phone' \
  -H 'accept: application/json'
```

#### Response

```json
{
  "query": "string",
  "suggestions": [
    {
      "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "text": "string",
      "type": "CUSTOMER"
    }
  ]
}
```

---

### 9. Get product detail

**`GET /api/v1/products/{id}`**

Get product detail

#### Request

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/products/6c5078d3-f8e3-4c88-9afe-48b5423c664f' \
  -H 'accept: application/json'
```

#### Response

```json
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
```

---

### 10. List product variants

**`GET /api/v1/products/{id}/variants`**

List product variants

#### Request

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/products/6c5078d3-f8e3-4c88-9afe-48b5423c664f/variants' \
  -H 'accept: application/json'
```

#### Response

```json
[
  {
    "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
    "productId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
    "sku": "SKU-12345",
    "name": "Example",
    "price": "250.00",
    "discountPrice": "250.00",
    "discountPercentage": 250.0,
    "barcode": "SAVE10",
    "weight": 250.0,
    "dimensions": "10x20x30 cm",
    "imageUrl": "string",
    "status": "ACTIVE",
    "isDefault": true,
    "sortOrder": 1,
    "stockQuantity": 1,
    "inStock": true,
    "options": [
      {
        "optionId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
        "optionName": "string",
        "valueId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
        "value": "10.00"
      }
    ]
  }
]
```

---

### 11. List product options

**`GET /api/v1/products/{id}/options`**

List product options

#### Request

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/products/6c5078d3-f8e3-4c88-9afe-48b5423c664f/options' \
  -H 'accept: application/json'
```

#### Response

```json
[
  {
    "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
    "productId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
    "name": "Example",
    "sortOrder": 1,
    "values": [
      {
        "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
        "optionId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
        "value": "10.00",
        "sortOrder": 1
      }
    ]
  }
]
```

---

### 12. Related products

**`GET /api/v1/products/{id}/related`**

Related products

#### Request

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/products/6c5078d3-f8e3-4c88-9afe-48b5423c664f/related' \
  -H 'accept: application/json'
```

#### Response

```json
{
  "strategy": "250.00",
  "sourceProductId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "items": [
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
  ]
}
```

---

### 13. Frequently bought together

**`GET /api/v1/products/{id}/also-bought`**

Frequently bought together

#### Request

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/products/6c5078d3-f8e3-4c88-9afe-48b5423c664f/also-bought' \
  -H 'accept: application/json'
```

#### Response

```json
{
  "strategy": "250.00",
  "sourceProductId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "items": [
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
  ]
}
```

---

### 14. Best-seller recommendations

**`GET /api/v1/products/recommendations`**

Best-seller recommendations

#### Request

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/products/recommendations' \
  -H 'accept: application/json'
```

#### Response

```json
{
  "strategy": "250.00",
  "sourceProductId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "items": [
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
  ]
}
```

---

### 15. Admin - Permanently delete any product

**`DELETE /api/v1/admin/products/{id}`**

Admin - Permanently delete any product

#### Request

```bash
curl -X 'DELETE' \
  'http://localhost:8080/api/v1/admin/products/6c5078d3-f8e3-4c88-9afe-48b5423c664f' \
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

### 16. Assign a product tax class

**`PUT /api/v1/admin/products/{id}/tax-class`**

Assign a product tax class

#### Request

```bash
curl -X 'PUT' \
  'http://localhost:8080/api/v1/admin/products/6c5078d3-f8e3-4c88-9afe-48b5423c664f/tax-class' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <admin_token>' \
  -H 'Content-Type: application/json' \
  -d '{
  "taxClassId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f"
}'
```

#### Response

```json
{
  "message": "Operation completed"
}
```

---

### 17. List a product's translations

**`GET /api/v1/admin/products/{id}/translations`**

List a product's translations

#### Request

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/admin/products/6c5078d3-f8e3-4c88-9afe-48b5423c664f/translations' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <admin_token>'
```

#### Response

```json
[
  {
    "productId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
    "locale": "en",
    "name": "Example",
    "description": "Looks good"
  }
]
```

---

### 18. Delete a product translation

**`DELETE /api/v1/admin/products/{id}/translations/{locale}`**

Delete a product translation

#### Request

```bash
curl -X 'DELETE' \
  'http://localhost:8080/api/v1/admin/products/6c5078d3-f8e3-4c88-9afe-48b5423c664f/translations/en' \
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

### 19. Create or update a product translation

**`PUT /api/v1/admin/products/{id}/translations/{locale}`**

Create or update a product translation

#### Request

```bash
curl -X 'PUT' \
  'http://localhost:8080/api/v1/admin/products/6c5078d3-f8e3-4c88-9afe-48b5423c664f/translations/en' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <admin_token>' \
  -H 'Content-Type: application/json' \
  -d '{
  "name": "Example",
  "description": "Looks good"
}'
```

#### Response

```json
{
  "productId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "locale": "en",
  "name": "Example",
  "description": "Looks good"
}
```

---

### 20. Create option dimension

**`POST /api/v1/seller/products/{id}/options`**

Create option dimension

#### Request

```bash
curl -X 'POST' \
  'http://localhost:8080/api/v1/seller/products/6c5078d3-f8e3-4c88-9afe-48b5423c664f/options' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <seller_token>' \
  -H 'Content-Type: application/json' \
  -d '{
  "name": "Example",
  "sortOrder": 1
}'
```

#### Response

```json
{
  "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "productId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "name": "Example",
  "sortOrder": 1,
  "values": [
    {
      "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "optionId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "value": "10.00",
      "sortOrder": 1
    }
  ]
}
```

---

### 21. Delete option dimension

**`DELETE /api/v1/seller/products/{id}/options/{optionId}`**

Delete option dimension

#### Request

```bash
curl -X 'DELETE' \
  'http://localhost:8080/api/v1/seller/products/6c5078d3-f8e3-4c88-9afe-48b5423c664f/options/6c5078d3-f8e3-4c88-9afe-48b5423c664f' \
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

### 22. Add option value

**`POST /api/v1/seller/products/{id}/options/{optionId}/values`**

Add option value

#### Request

```bash
curl -X 'POST' \
  'http://localhost:8080/api/v1/seller/products/6c5078d3-f8e3-4c88-9afe-48b5423c664f/options/6c5078d3-f8e3-4c88-9afe-48b5423c664f/values' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <seller_token>' \
  -H 'Content-Type: application/json' \
  -d '{
  "value": "10.00",
  "sortOrder": 1
}'
```

#### Response

```json
{
  "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "optionId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "value": "10.00",
  "sortOrder": 1
}
```

---

### 23. Delete option value

**`DELETE /api/v1/seller/products/{id}/options/{optionId}/values/{valueId}`**

Delete option value

#### Request

```bash
curl -X 'DELETE' \
  'http://localhost:8080/api/v1/seller/products/6c5078d3-f8e3-4c88-9afe-48b5423c664f/options/6c5078d3-f8e3-4c88-9afe-48b5423c664f/values/6c5078d3-f8e3-4c88-9afe-48b5423c664f' \
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

### 24. Create product variant

**`POST /api/v1/seller/products/{id}/variants`**

Create product variant

#### Request

```bash
curl -X 'POST' \
  'http://localhost:8080/api/v1/seller/products/6c5078d3-f8e3-4c88-9afe-48b5423c664f/variants' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <seller_token>' \
  -H 'Content-Type: application/json' \
  -d '{
  "sku": "SKU-12345",
  "name": "Example",
  "price": "250.00",
  "discountPrice": "250.00",
  "barcode": "SAVE10",
  "weight": "string",
  "dimensions": "10x20x30 cm",
  "imageUrl": "string",
  "status": "ACTIVE",
  "isDefault": true,
  "sortOrder": 1
}'
```

#### Response

```json
{
  "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "productId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "sku": "SKU-12345",
  "name": "Example",
  "price": "250.00",
  "discountPrice": "250.00",
  "discountPercentage": 250.0,
  "barcode": "SAVE10",
  "weight": 250.0,
  "dimensions": "10x20x30 cm",
  "imageUrl": "string",
  "status": "ACTIVE",
  "isDefault": true,
  "sortOrder": 1,
  "stockQuantity": 1,
  "inStock": true,
  "options": [
    {
      "optionId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "optionName": "string",
      "valueId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "value": "10.00"
    }
  ]
}
```

---

### 25. Delete product variant

**`DELETE /api/v1/seller/products/{id}/variants/{variantId}`**

Delete product variant

#### Request

```bash
curl -X 'DELETE' \
  'http://localhost:8080/api/v1/seller/products/6c5078d3-f8e3-4c88-9afe-48b5423c664f/variants/6c5078d3-f8e3-4c88-9afe-48b5423c664f' \
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

### 26. Update product variant

**`PUT /api/v1/seller/products/{id}/variants/{variantId}`**

Update product variant

#### Request

```bash
curl -X 'PUT' \
  'http://localhost:8080/api/v1/seller/products/6c5078d3-f8e3-4c88-9afe-48b5423c664f/variants/6c5078d3-f8e3-4c88-9afe-48b5423c664f' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <seller_token>' \
  -H 'Content-Type: application/json' \
  -d '{
  "sku": "SKU-12345",
  "name": "Example",
  "price": "250.00",
  "discountPrice": "250.00",
  "barcode": "SAVE10",
  "weight": "string",
  "dimensions": "10x20x30 cm",
  "imageUrl": "string",
  "status": "ACTIVE",
  "isDefault": true,
  "sortOrder": 1
}'
```

#### Response

```json
{
  "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "productId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "sku": "SKU-12345",
  "name": "Example",
  "price": "250.00",
  "discountPrice": "250.00",
  "discountPercentage": 250.0,
  "barcode": "SAVE10",
  "weight": 250.0,
  "dimensions": "10x20x30 cm",
  "imageUrl": "string",
  "status": "ACTIVE",
  "isDefault": true,
  "sortOrder": 1,
  "stockQuantity": 1,
  "inStock": true,
  "options": [
    {
      "optionId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "optionName": "string",
      "valueId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "value": "10.00"
    }
  ]
}
```

---

### 27. Attach option values to a variant

**`PUT /api/v1/seller/products/{id}/variants/{variantId}/options`**

Attach option values to a variant

#### Request

```bash
curl -X 'PUT' \
  'http://localhost:8080/api/v1/seller/products/6c5078d3-f8e3-4c88-9afe-48b5423c664f/variants/6c5078d3-f8e3-4c88-9afe-48b5423c664f/options' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <seller_token>' \
  -H 'Content-Type: application/json' \
  -d '{
  "valueIds": [
    "string"
  ]
}'
```

#### Response

```json
{
  "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "productId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "sku": "SKU-12345",
  "name": "Example",
  "price": "250.00",
  "discountPrice": "250.00",
  "discountPercentage": 250.0,
  "barcode": "SAVE10",
  "weight": 250.0,
  "dimensions": "10x20x30 cm",
  "imageUrl": "string",
  "status": "ACTIVE",
  "isDefault": true,
  "sortOrder": 1,
  "stockQuantity": 1,
  "inStock": true,
  "options": [
    {
      "optionId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "optionName": "string",
      "valueId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "value": "10.00"
    }
  ]
}
```

---

### 28. List back-in-stock alerts

**`GET /api/v1/stock-alerts`**

List back-in-stock alerts

#### Request

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/stock-alerts' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <access_token>'
```

#### Response

```json
[
  {
    "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
    "productId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
    "variantId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
    "notifiedAt": "2024-05-06T12:00:00",
    "createdAt": "2024-05-06T12:00:00"
  }
]
```

---

### 29. Subscribe to a back-in-stock alert

**`POST /api/v1/stock-alerts`**

Subscribe to a back-in-stock alert

#### Request

```bash
curl -X 'POST' \
  'http://localhost:8080/api/v1/stock-alerts' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <access_token>' \
  -H 'Content-Type: application/json' \
  -d '{
  "productId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "variantId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f"
}'
```

#### Response

```json
{
  "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "productId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "variantId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "notifiedAt": "2024-05-06T12:00:00",
  "createdAt": "2024-05-06T12:00:00"
}
```

---

### 30. Remove a back-in-stock alert

**`DELETE /api/v1/stock-alerts/{productId}`**

Remove a back-in-stock alert

#### Request

```bash
curl -X 'DELETE' \
  'http://localhost:8080/api/v1/stock-alerts/6c5078d3-f8e3-4c88-9afe-48b5423c664f' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <access_token>'
```

#### Response

```json
{
  "message": "Operation completed"
}
```

---

## Error Handling

## Public catalog and recommendations

| Method | Endpoint | Notes |
|---|---|---|
| `GET` | `/api/v1/products` | Offset pagination (`page`/`perPage` or `limit`/`offset`) with the same filters as search |
| `GET` | `/api/v1/products/{id}/related` | Same category/brand, excludes the current product |
| `GET` | `/api/v1/products/{id}/also-bought` | Co-purchase feed from paid order history |
| `GET` | `/api/v1/products/recommendations` | Best sellers, accepts `limit` (default 10) |
| `GET` | `/api/v1/products/{id}/variants` | Purchasable variants with per-variant stock |
| `GET` | `/api/v1/products/{id}/options` | Option dimensions (Size, Color) and their values |

```bash
curl -X 'GET' 'http://localhost:8080/api/v1/products?page=1&perPage=20'
curl -X 'GET' 'http://localhost:8080/api/v1/products/6c5078d3-f8e3-4c88-9afe-48b5423c664f/related'
```

See [Product Variants](product-variants.md) for seller-side variant/option management.

### Success Responses
- **HTTP 200/201**: Success.

### Common Error Codes
| Status Code | Description |
|-------------|-------------|
| `400` | Bad Request (Validation error) |
| `401` | Unauthorized (Invalid token) |
| `403` | Forbidden (User is not the owner) |
| `404` | Not Found |
