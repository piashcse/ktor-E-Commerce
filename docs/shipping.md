# Shipping API

This documentation provides comprehensive details for the Shipping API endpoints. The API supports creating, retrieving, updating, and deleting shipping addresses within the platform. Shipping addresses are associated with specific orders and users for accurate delivery management.

**Base URL:** `http://localhost:8080`

## Authentication

All Shipping endpoints require Bearer token authentication. Include the access token in the Authorization header:

```
Authorization: Bearer <your_access_token>
```

### Shipping Endpoints

| Method | Endpoint | Description | Authentication Required |
|--------|----------|-------------|------------------------|
| `GET` | `/shipping` | Retrieve shipping information by order ID | Yes |
| `POST` | `/shipping` | Create a new shipping address | Yes |
| `PUT` | `/shipping/{id}` | Update an existing shipping address | Yes |
| `DELETE` | `/shipping/{id}` | Delete a shipping address | Yes |

---

## Endpoint Details

### 1. Get Shipping Information

**`GET /shipping`**

Retrieve shipping information associated with a specific order ID.

#### Query Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `orderId` | string | Yes | UUID of the order to get shipping information for |

#### Headers

| Header | Value | Required |
|--------|-------|----------|
| `Authorization` | `Bearer <access_token>` | Yes |

#### Example Request

```bash
curl -X 'GET' \
  'http://localhost:8080/shipping?orderId=c7f38846-4f63-460f-b956-f2b6758dbffd' \
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
    "id": "5489a8b4-7a16-4854-b157-396a8a731032",
    "userId": "a67fd0cc-3d92-4259-bbd4-1e0ba49dece4",
    "orderId": "c7f38846-4f63-460f-b956-f2b6758dbffd",
    "shipAddress": "update address",
    "shipCity": "Dhaka",
    "shipPhone": 1073741824,
    "shipName": "paperfly",
    "shipEmail": "customer@gmail.com",
    "shipCountry": "Bangladesh"
  }
}
```

#### Response Fields

| Field | Type | Description |
|-------|------|-------------|
| `data.id` | string | Unique identifier for the shipping record |
| `data.userId` | string | UUID of the user associated with the shipping |
| `data.orderId` | string | UUID of the order associated with the shipping |
| `data.shipAddress` | string | Shipping address |
| `data.shipCity` | string | City for shipping |
| `data.shipPhone` | number | Phone number for shipping contact |
| `data.shipName` | string | Shipping method name |
| `data.shipEmail` | string | Email for shipping contact |
| `data.shipCountry` | string | Country for shipping |

---

### 2. Create Shipping Address

**`POST /shipping`**

Create a new shipping address for a specific order.

#### Headers

| Header | Value | Required |
|--------|-------|----------|
| `Authorization` | `Bearer <access_token>` | Yes |
| `Content-Type` | `application/json` | Yes |

#### Request Body

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `orderId` | string | Yes | UUID of the order for this shipping |
| `shipAddress` | string | Yes | Shipping address |
| `shipCity` | string | Yes | City for shipping |
| `shipCountry` | string | Yes | Country for shipping |
| `shipEmail` | string | Yes | Email for shipping contact |
| `shipName` | string | Yes | Shipping method name |
| `shipPhone` | number | Yes | Phone number for shipping contact |

#### Example Request

```bash
curl -X 'POST' \
  'http://localhost:8080/shipping' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9...' \
  -H 'Content-Type: application/json' \
  -d '{
  "orderId": "7e49b2a1-fa0c-4aac-b996-91f2411f14b7",
  "shipAddress": "Dhaka Bangladesh",
  "shipCity": "Dhaka",
  "shipCountry": "Bangladesh",
  "shipEmail": "customer@gmail.com",
  "shipName": "string",
  "shipPhone": 1073741824
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
    "id": "471ebc82-80e7-4da0-a472-d1c8835f57b8",
    "orderId": "7e49b2a1-fa0c-4aac-b996-91f2411f14b7",
    "shipAddress": "Dhaka Bangladesh",
    "shipCity": "Dhaka",
    "shipPhone": 1073741824,
    "shipName": "string",
    "shipEmail": "customer@gmail.com",
    "shipCountry": "Bangladesh"
  }
}
```

#### Response Fields

| Field | Type | Description |
|-------|------|-------------|
| `data.id` | string | Unique identifier for the created shipping record |
| `data.orderId` | string | UUID of the order associated with the shipping |
| `data.shipAddress` | string | Shipping address |
| `data.shipCity` | string | City for shipping |
| `data.shipPhone` | number | Phone number for shipping contact |
| `data.shipName` | string | Shipping method name |
| `data.shipEmail` | string | Email for shipping contact |
| `data.shipCountry` | string | Country for shipping |

---

### 3. Update Shipping Address

**`PUT /shipping/{id}`**

Update an existing shipping address by its ID. You can modify any of the shipping details.

#### Path Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `id` | string | Yes | Unique identifier of the shipping address to update |

#### Query Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `shipAddress` | string | No | New shipping address |
| `shipCity` | string | No | New city for shipping |
| `shipPhone` | number | No | New phone number for shipping contact |
| `shipName` | string | No | New shipping method name |
| `shipEmail` | string | No | New email for shipping contact |
| `shipCountry` | string | No | New country for shipping |

#### Headers

| Header | Value | Required |
|--------|-------|----------|
| `Authorization` | `Bearer <access_token>` | Yes |

#### Example Request

```bash
curl -X 'PUT' \
  'http://localhost:8080/shipping/5489a8b4-7a16-4854-b157-396a8a731032?shipAddress=Updated%20shipping%20address' \
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
    "id": "5489a8b4-7a16-4854-b157-396a8a731032",
    "userId": "a67fd0cc-3d92-4259-bbd4-1e0ba49dece4",
    "orderId": "c7f38846-4f63-460f-b956-f2b6758dbffd",
    "shipAddress": "Updated shipping address",
    "shipCity": "Dhaka",
    "shipPhone": 1073741824,
    "shipName": "paperfly",
    "shipEmail": "customer@gmail.com",
    "shipCountry": "Bangladesh"
  }
}
```

#### Response Fields

| Field | Type | Description |
|-------|------|-------------|
| `data.id` | string | Unique identifier of the updated shipping record |
| `data.userId` | string | UUID of the user associated with the shipping |
| `data.orderId` | string | UUID of the order associated with the shipping |
| `data.shipAddress` | string | Updated shipping address |
| `data.shipCity` | string | Updated city for shipping |
| `data.shipPhone` | number | Updated phone number for shipping contact |
| `data.shipName` | string | Updated shipping method name |
| `data.shipEmail` | string | Updated email for shipping contact |
| `data.shipCountry` | string | Updated country for shipping |

---

### 4. Delete Shipping Address

**`DELETE /shipping/{id}`**

Delete a shipping address by its ID. This operation permanently removes the shipping address from the system.

#### Path Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `id` | string | Yes | Unique identifier of the shipping address to delete |

#### Headers

| Header | Value | Required |
|--------|-------|----------|
| `Authorization` | `Bearer <access_token>` | Yes |

#### Example Request

```bash
curl -X 'DELETE' \
  'http://localhost:8080/shipping/471ebc82-80e7-4da0-a472-d1c8835f57b8' \
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
  "data": "471ebc82-80e7-4da0-a472-d1c8835f57b8"
}
```

#### Response Fields

| Field | Type | Description |
|-------|------|-------------|
| `data` | string | ID of the deleted shipping address |

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
| `404` | Not Found - Shipping address or order not found |
| `409` | Conflict - Invalid shipping data or order conflict |
| `500` | Internal Server Error - Server error |