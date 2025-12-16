# Product Special API

This documentation provides comprehensive details for the Product Special API endpoints. The API supports retrieving special product categories such as featured products, best-selling products, and hot deals. These endpoints help highlight special products to customers and improve discovery of popular or discounted items.

**Base URL:** `http://localhost:8080`

## Authentication

Product special endpoints do not require authentication, allowing public access to featured, best-selling, and hot deal products.

### Product Special Endpoints

| Method | Endpoint | Description | Authentication Required |
|--------|----------|-------------|------------------------|
| `GET` | `/product/featured` | Retrieve featured products | No |
| `GET` | `/product/best-selling` | Retrieve best selling products | No |
| `GET` | `/product/hot-deals` | Retrieve products on hot deals | No |

---

## Endpoint Details

### 1. Get Featured Products

**`GET /product/featured`**

Retrieve a list of featured products that are highlighted for promotional purposes.

#### Headers

| Header | Value | Required |
|--------|-------|----------|
| `accept` | `application/json` | Yes |

#### Example Request

```bash
curl -X 'GET' \
  'http://localhost:8080/product/featured' \
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
      "name": "Featured Product",
      "price": 99.99,
      "status": "ACTIVE",
      "featured": true
    }
  ]
}
```

#### Response Fields

| Field | Type | Description |
|-------|------|-------------|
| `data` | array | Array of featured product objects |
| `data[].id` | string | Unique identifier for the product |
| `data[].name` | string | Name of the product |
| `data[].price` | number | Price of the product |
| `data[].status` | string | Status of the product (ACTIVE, INACTIVE, etc.) |
| `data[].featured` | boolean | Whether the product is featured |

---

### 2. Get Best Selling Products

**`GET /product/best-selling`**

Retrieve a list of best selling products based on sales data.

#### Headers

| Header | Value | Required |
|--------|-------|----------|
| `accept` | `application/json` | Yes |

#### Example Request

```bash
curl -X 'GET' \
  'http://localhost:8080/product/best-selling' \
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
      "name": "Best Selling Product",
      "price": 79.99,
      "status": "ACTIVE",
      "bestSelling": true
    }
  ]
}
```

#### Response Fields

| Field | Type | Description |
|-------|------|-------------|
| `data` | array | Array of best selling product objects |
| `data[].id` | string | Unique identifier for the product |
| `data[].name` | string | Name of the product |
| `data[].price` | number | Price of the product |
| `data[].status` | string | Status of the product (ACTIVE, INACTIVE, etc.) |
| `data[].bestSelling` | boolean | Whether the product is a best seller |

---

### 3. Get Hot Deal Products

**`GET /product/hot-deals`**

Retrieve a list of products currently on hot deals with special pricing or offers.

#### Headers

| Header | Value | Required |
|--------|-------|----------|
| `accept` | `application/json` | Yes |

#### Example Request

```bash
curl -X 'GET' \
  'http://localhost:8080/product/hot-deals' \
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
      "name": "Hot Deal Product",
      "price": 59.99,
      "discountPrice": 49.99,
      "status": "ACTIVE",
      "hotDeal": true
    }
  ]
}
```

#### Response Fields

| Field | Type | Description |
|-------|------|-------------|
| `data` | array | Array of hot deal product objects |
| `data[].id` | string | Unique identifier for the product |
| `data[].name` | string | Name of the product |
| `data[].price` | number | Regular price of the product |
| `data[].discountPrice` | number | Discounted price of the product |
| `data[].status` | string | Status of the product (ACTIVE, INACTIVE, etc.) |
| `data[].hotDeal` | boolean | Whether the product is a hot deal |

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
| `400` | Bad Request - Invalid parameters |
| `404` | Not Found - No products found matching the criteria |
| `500` | Internal Server Error - Server error |

---

## Special Product Management Guidelines

### Featured Products
- Selected by administrators for promotional exposure
- Typically high-quality or premium products
- Limited number shown to maintain exclusivity

### Best Selling Products
- Calculated based on sales volume and frequency
- Automatically updated based on market performance
- Helps customers identify popular choices

### Hot Deal Products
- Products with special discounts or offers
- Time-limited promotions
- Attracts price-sensitive customers