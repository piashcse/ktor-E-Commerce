# Product Seller API

This documentation provides comprehensive details for the Product Seller API endpoints. The API supports retrieving products specifically for sellers, allowing them to manage their own product listings. These endpoints are designed to give sellers access to their own products with appropriate filtering and pagination options.

**Base URL:** `http://localhost:8080`

## Authentication

Product seller endpoints require authentication. Sellers can only access their own products, and the token must belong to the seller who owns the products being accessed.

### Product Seller Endpoints

| Method | Endpoint | Description | Authentication Required |
|--------|----------|-------------|------------------------|
| `GET` | `/product/seller` | Retrieve products belonging to the authenticated seller | Yes |

---

## Endpoint Details

### 1. Get Seller Products

**`GET /product/seller`**

Retrieve a list of products belonging to the authenticated seller with optional filters.

#### Query Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `limit` | number | No | Maximum number of products to return (default: 10) |
| `maxPrice` | number | No | Maximum price filter |
| `minPrice` | number | No | Minimum price filter |
| `categoryId` | string | No | UUID of the category to filter by |
| `subCategoryId` | string | No | UUID of the subcategory to filter by |
| `brandId` | string | No | UUID of the brand to filter by |

#### Headers

| Header | Value | Required |
|--------|-------|----------|
| `Authorization` | `Bearer <access_token>` | Yes |

#### Example Request

```bash
curl -X 'GET' \
  'http://localhost:8080/product/seller?limit=10&maxPrice=100&minPrice=0&categoryId=5e67ec97-9ed6-48ee-9d56-4163fe1711cb&subCategoryId=70ac842b-7a81-4976-9564-d440880d1736&brandId=28918963-f932-425b-884b-a34d8ae69b2a' \
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
      "id": "cbd630f6-bf9f-48ad-ac51-f806807d99fd",
      "name": "Product Name",
      "price": 99.99,
      "status": "ACTIVE"
    }
  ]
}
```

#### Response Fields

| Field | Type | Description |
|-------|------|-------------|
| `data` | array | Array of product objects belonging to the seller |
| `data[].id` | string | Unique identifier for the product |
| `data[].name` | string | Name of the product |
| `data[].price` | number | Price of the product |
| `data[].status` | string | Status of the product (ACTIVE, INACTIVE, etc.) |

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
| `403` | Forbidden - Insufficient privileges (not the product owner) |
| `404` | Not Found - No products found for the seller |
| `500` | Internal Server Error - Server error |

---

## Seller Product Access Guidelines

### Access Control
- Sellers can only access products they own
- Authentication token determines which seller is making the request
- No other users (customers, other sellers, admins) can access these endpoints

### Filtering Options
- All product filters available: category, subcategory, brand, price range
- Filters are applied to the seller's own products only
- Pagination helps with performance when a seller has many products