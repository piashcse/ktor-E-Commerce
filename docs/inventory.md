# Inventory API

This documentation provides comprehensive details for the Inventory API endpoints. The API supports creating, retrieving, updating, and managing inventory records within the platform. Inventory records are associated with specific products and track available quantities, reserved quantities, and overall stock levels.

**Base URL:** `http://localhost:8080`

## Authentication

All Inventory endpoints require Bearer token authentication for sellers and admins. Include the access token in the Authorization header:

```
Authorization: Bearer <your_access_token>
```

### Inventory Endpoints

| Method | Endpoint | Description | Authentication Required |
|--------|----------|-------------|------------------------|
| `POST` | `/inventory` | Create a new inventory record | Yes |
| `PUT` | `/inventory/{id}` | Update an existing inventory record | Yes |
| `PUT` | `/inventory/stock/{id}` | Update product stock quantity | Yes |
| `GET` | `/inventory/{id}` | Retrieve inventory details for a product | Yes |
| `GET` | `/inventory/shop` | Retrieve inventory records for a shop | Yes |
| `GET` | `/inventory/low-stock` | Retrieve inventory records with low stock | Yes |

---

## Endpoint Details

### 1. Create Inventory Record

**`POST /inventory`**

Create a new inventory record for a specific product with initial quantities.

#### Headers

| Header | Value | Required |
|--------|-------|----------|
| `Authorization` | `Bearer <access_token>` | Yes |
| `Content-Type` | `application/json` | Yes |

#### Request Body

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `productId` | string | Yes | UUID of the product this inventory is for |
| `quantity` | number | Yes | Total quantity in inventory |
| `reserved` | number | No | Quantity that is currently reserved |

#### Example Request

```bash
curl -X 'POST' \
  'http://localhost:8080/inventory' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9...' \
  -H 'Content-Type: application/json' \
  -d '{
  "productId": "cbd630f6-bf9f-48ad-ac51-f806807d99fd",
  "quantity": 100,
  "reserved": 10
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
    "id": "12345678-1234-1234-1234-123456789012",
    "productId": "cbd630f6-bf9f-48ad-ac51-f806807d99fd",
    "quantity": 100,
    "reserved": 10,
    "available": 90
  }
}
```

#### Response Fields

| Field | Type | Description |
|-------|------|-------------|
| `data.id` | string | Unique identifier for the created inventory record |
| `data.productId` | string | UUID of the product this inventory is for |
| `data.quantity` | number | Total quantity in inventory |
| `data.reserved` | number | Quantity that is currently reserved |
| `data.available` | number | Available quantity (quantity - reserved) |

---

### 2. Update Inventory Record

**`PUT /inventory/{id}`**

Update an existing inventory record by its ID. You can modify the quantity and reserved amounts.

#### Path Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `id` | string | Yes | Unique identifier of the inventory record to update |

#### Headers

| Header | Value | Required |
|--------|-------|----------|
| `Authorization` | `Bearer <access_token>` | Yes |
| `Content-Type` | `application/json` | Yes |

#### Request Body

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `productId` | string | No | UUID of the product this inventory is for |
| `quantity` | number | No | Total quantity in inventory |
| `reserved` | number | No | Quantity that is currently reserved |

#### Example Request

```bash
curl -X 'PUT' \
  'http://localhost:8080/inventory/12345678-1234-1234-1234-123456789012' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9...' \
  -H 'Content-Type: application/json' \
  -d '{
  "productId": "cbd630f6-bf9f-48ad-ac51-f806807d99fd",
  "quantity": 150,
  "reserved": 20
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
    "id": "12345678-1234-1234-1234-123456789012",
    "productId": "cbd630f6-bf9f-48ad-ac51-f806807d99fd",
    "quantity": 150,
    "reserved": 20,
    "available": 130
  }
}
```

#### Response Fields

| Field | Type | Description |
|-------|------|-------------|
| `data.id` | string | Unique identifier of the updated inventory record |
| `data.productId` | string | UUID of the product this inventory is for |
| `data.quantity` | number | Updated total quantity in inventory |
| `data.reserved` | number | Updated quantity that is currently reserved |
| `data.available` | number | Available quantity (quantity - reserved) |

---

### 3. Update Product Stock Quantity

**`PUT /inventory/stock/{id}`**

Update the stock quantity for a specific product with addition or subtraction operations.

#### Path Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `id` | string | Yes | Product ID to update stock for |

#### Query Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `quantity` | number | Yes | Amount to add or subtract |
| `operation` | string | Yes | Operation to perform: "add" or "subtract" |

#### Headers

| Header | Value | Required |
|--------|-------|----------|
| `Authorization` | `Bearer <access_token>` | Yes |

#### Example Request

```bash
curl -X 'PUT' \
  'http://localhost:8080/inventory/stock/cbd630f6-bf9f-48ad-ac51-f806807d99fd?quantity=50&operation=add' \
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
  "data": {
    "id": "cbd630f6-bf9f-48ad-ac51-f806807d99fd",
    "newQuantity": 200,
    "operation": "add"
  }
}
```

#### Response Fields

| Field | Type | Description |
|-------|------|-------------|
| `data.id` | string | Product ID that was updated |
| `data.newQuantity` | number | New quantity after the operation |
| `data.operation` | string | Operation that was performed ("add" or "subtract") |

---

### 4. Get Inventory Details

**`GET /inventory/{id}`**

Retrieve detailed inventory information for a specific product.

#### Path Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `id` | string | Yes | Product ID to get inventory details for |

#### Headers

| Header | Value | Required |
|--------|-------|----------|
| `Authorization` | `Bearer <access_token>` | Yes |

#### Example Request

```bash
curl -X 'GET' \
  'http://localhost:8080/inventory/cbd630f6-bf9f-48ad-ac51-f806807d99fd' \
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
  "data": {
    "id": "cbd630f6-bf9f-48ad-ac51-f806807d99fd",
    "productId": "cbd630f6-bf9f-48ad-ac51-f806807d99fd",
    "quantity": 200,
    "reserved": 20,
    "available": 180
  }
}
```

#### Response Fields

| Field | Type | Description |
|-------|------|-------------|
| `data.id` | string | Unique identifier for the inventory record |
| `data.productId` | string | UUID of the product this inventory is for |
| `data.quantity` | number | Total quantity in inventory |
| `data.reserved` | number | Quantity that is currently reserved |
| `data.available` | number | Available quantity (quantity - reserved) |

---

### 5. Get Shop Inventory

**`GET /inventory/shop`**

Retrieve inventory records associated with a specific shop.

#### Query Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `shopId` | string | Yes | UUID of the shop to get inventory for |

#### Headers

| Header | Value | Required |
|--------|-------|----------|
| `Authorization` | `Bearer <access_token>` | Yes |

#### Example Request

```bash
curl -X 'GET' \
  'http://localhost:8080/inventory/shop?shopId=12345678-1234-1234-1234-123456789012' \
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
      "id": "12345678-1234-1234-1234-123456789012",
      "productId": "cbd630f6-bf9f-48ad-ac51-f806807d99fd",
      "quantity": 200,
      "reserved": 20,
      "available": 180
    }
  ]
}
```

#### Response Fields

| Field | Type | Description |
|-------|------|-------------|
| `data` | array | Array of inventory record objects |
| `data[].id` | string | Unique identifier for the inventory record |
| `data[].productId` | string | UUID of the product this inventory is for |
| `data[].quantity` | number | Total quantity in inventory |
| `data[].reserved` | number | Quantity that is currently reserved |
| `data[].available` | number | Available quantity (quantity - reserved) |

---

### 6. Get Low Stock Inventory

**`GET /inventory/low-stock`**

Retrieve inventory records that have low stock levels, typically used for restocking alerts.

#### Headers

| Header | Value | Required |
|--------|-------|----------|
| `Authorization` | `Bearer <access_token>` | Yes |

#### Example Request

```bash
curl -X 'GET' \
  'http://localhost:8080/inventory/low-stock' \
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
      "id": "12345678-1234-1234-1234-123456789012",
      "productId": "cbd630f6-bf9f-48ad-ac51-f806807d99fd",
      "quantity": 5,
      "reserved": 2,
      "available": 3
    }
  ]
}
```

#### Response Fields

| Field | Type | Description |
|-------|------|-------------|
| `data` | array | Array of inventory record objects with low stock |
| `data[].id` | string | Unique identifier for the inventory record |
| `data[].productId` | string | UUID of the product this inventory is for |
| `data[].quantity` | number | Total quantity in inventory |
| `data[].reserved` | number | Quantity that is currently reserved |
| `data[].available` | number | Available quantity (quantity - reserved) |

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
| `400` | Bad Request - Invalid parameters or negative quantity |
| `401` | Unauthorized - Invalid or missing authentication |
| `403` | Forbidden - Insufficient privileges |
| `404` | Not Found - Product or inventory record not found |
| `409` | Conflict - Product out of stock or invalid quantity operation |
| `500` | Internal Server Error - Server error |

---

## Inventory Management Guidelines

### Stock Operations
- Always verify product permissions before updating inventory
- Use the stock update endpoint for simple quantity changes
- Consider reserved quantities when calculating available stock
- Regular monitoring for low stock items is recommended

### Inventory Data
- Quantity represents total stock in the system
- Reserved quantity tracks items allocated to pending orders
- Available quantity is calculated as (quantity - reserved)
- Low stock threshold is typically determined by business rules