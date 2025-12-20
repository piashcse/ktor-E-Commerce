# Product Search API

This documentation provides comprehensive details for the Product Search API endpoints. The API supports searching for products with various filters including name, category, price range, and more. These endpoints enable users to discover products efficiently using different search criteria.

**Base URL:** `http://localhost:8080`

## Authentication

Product search endpoints do not require authentication, allowing public access to search for products.

### Product Search Endpoints

| Method | Endpoint | Description | Authentication Required |
|--------|----------|-------------|------------------------|
| `GET` | `/product/search` | Search products with filters | No |

---

## Endpoint Details

### 1. Search Products

**`GET /product/search`**

Search for products using various filters including name, category, price range, and other attributes.

#### Query Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `limit` | number | No | Maximum number of products to return (default: 10) |
| `name` | string | No | Search term to match against product names |
| `categoryId` | string | No | UUID of the category to filter by |
| `maxPrice` | number | No | Maximum price filter |
| `minPrice` | number | No | Minimum price filter |
| `subCategoryId` | string | No | UUID of the subcategory to filter by |
| `brandId` | string | No | UUID of the brand to filter by |

#### Headers

| Header | Value | Required |
|--------|-------|----------|
| `accept` | `application/json` | Yes |

#### Example Request

```bash
curl -X 'GET' \
  'http://localhost:8080/product/search?limit=10&name=smartphone&categoryId=5e67ec97-9ed6-48ee-9d56-4163fe1711cb&maxPrice=1000&minPrice=10' \
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
      "id": "cbd630f6-bf9f-48ad-ac51-f806807d99fd",
      "name": "Smartphone",
      "price": 599.99,
      "status": "ACTIVE"
    }
  ]
}
```

#### Response Fields

| Field | Type | Description |
|-------|------|-------------|
| `data` | array | Array of product objects matching the search criteria |
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
| `404` | Not Found - No products found matching the criteria |
| `500` | Internal Server Error - Server error |

---

## Search Guidelines

### Search Performance
- Limit the number of results using the `limit` parameter to improve performance
- Use specific search terms for more accurate results
- Combine multiple filters for refined search results

### Searchable Fields
- Product name is the primary search field
- Category and subcategory filters help narrow results
- Price range filters can be combined with other criteria
- Brand filtering allows for branded product searches