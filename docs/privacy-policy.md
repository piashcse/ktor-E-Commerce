# Privacy Policy API

This documentation provides comprehensive details for the Privacy Policy API endpoints. The API supports creating, retrieving, updating, and managing privacy policies and terms of conditions within the platform. Policies can be versioned, activated/deactivated, and managed by administrators.

**Base URL:** `http://localhost:8080`

## Authentication

Most Policy endpoints require Bearer token authentication. Include the access token in the Authorization header:

```
Authorization: Bearer <your_access_token>
```

### Policy Endpoints

| Method | Endpoint | Description | Authentication Required |
|--------|----------|-------------|------------------------|
| `GET` | `/policy` | Retrieve list of all policies | No |
| `GET` | `/policy/{type}` | Retrieve policy by type | No |
| `GET` | `/policy/detail/{id}` | Retrieve policy by ID | No |
| `POST` | `/policy` | Create a new policy | Yes |
| `PUT` | `/policy/{id}` | Update an existing policy | Yes |
| `POST` | `/policy/deactivate/{id}` | Deactivate a policy | Yes |
| `POST` | `/policy-consent` | Create a policy consent record | Yes |
| `GET` | `/policy-consents` | Retrieve all user consent records | Yes |
| `GET` | `/policy-consents/{policyType}` | Check if user has consented to specific policy type | Yes |

---

## Endpoint Details

### 1. Get All Policies

**`GET /policy`**

Retrieve a list of all policies including Privacy Policies and Terms & Conditions.

#### Headers

| Header | Value | Required |
|--------|-------|----------|
| `accept` | `application/json` | Yes |

#### Example Request

```bash
curl -X 'GET' \
  'http://localhost:8080/policy' \
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
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "type": "PRIVACY_POLICY",
      "title": "Privacy Policy",
      "content": "This is our privacy policy...",
      "version": "1.0",
      "effectiveDate": "2023-01-01T00:00:00Z",
      "isActive": true,
      "createdAt": "2023-01-01T00:00:00Z",
      "updatedAt": "2023-01-01T00:00:00Z"
    },
    {
      "id": "550e8400-e29b-41d4-a716-446655440001",
      "type": "TERMS_CONDITIONS",
      "title": "Terms and Conditions",
      "content": "These are our terms and conditions...",
      "version": "1.0",
      "effectiveDate": "2023-01-01T00:00:00Z",
      "isActive": true,
      "createdAt": "2023-01-01T00:00:00Z",
      "updatedAt": "2023-01-01T00:00:00Z"
    }
  ]
}
```

#### Response Fields

| Field | Type | Description |
|-------|------|-------------|
| `data` | array | Array of policy objects |
| `data[].id` | string | Unique identifier for the policy |
| `data[].type` | string | Policy type (PRIVACY_POLICY, TERMS_CONDITIONS) |
| `data[].title` | string | Title of the policy |
| `data[].content` | string | Full content of the policy |
| `data[].version` | string | Version number of the policy |
| `data[].effectiveDate` | string | ISO 8601 date when policy becomes effective |
| `data[].isActive` | boolean | Whether the policy is currently active |
| `data[].createdAt` | string | ISO 8601 timestamp of creation |
| `data[].updatedAt` | string | ISO 8601 timestamp of last update |

---

### 2. Get Policy by Type

**`GET /policy/{type}`**

Retrieve a specific policy by its type (PRIVACY_POLICY or TERMS_CONDITIONS).

#### Path Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `type` | string | Yes | Policy type (PRIVACY_POLICY or TERMS_CONDITIONS) |

#### Headers

| Header | Value | Required |
|--------|-------|----------|
| `accept` | `application/json` | Yes |

#### Example Request

```bash
curl -X 'GET' \
  'http://localhost:8080/policy/PRIVACY_POLICY' \
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
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "type": "PRIVACY_POLICY",
    "title": "Privacy Policy",
    "content": "This is our privacy policy...",
    "version": "1.0",
    "effectiveDate": "2023-01-01T00:00:00Z",
    "isActive": true,
    "createdAt": "2023-01-01T00:00:00Z",
    "updatedAt": "2023-01-01T00:00:00Z"
  }
}
```

#### Response Fields

| Field | Type | Description |
|-------|------|-------------|
| `data.id` | string | Unique identifier for the policy |
| `data.type` | string | Policy type |
| `data.title` | string | Title of the policy |
| `data.content` | string | Full content of the policy |
| `data.version` | string | Version number of the policy |
| `data.effectiveDate` | string | ISO 8601 date when policy becomes effective |
| `data.isActive` | boolean | Whether the policy is currently active |
| `data.createdAt` | string | ISO 8601 timestamp of creation |
| `data.updatedAt` | string | ISO 8601 timestamp of last update |

---

### 3. Get Policy by ID

**`GET /policy/detail/{id}`**

Retrieve a specific policy by its unique identifier.

#### Path Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `id` | string | Yes | Unique identifier of the policy |

#### Headers

| Header | Value | Required |
|--------|-------|----------|
| `accept` | `application/json` | Yes |

#### Example Request

```bash
curl -X 'GET' \
  'http://localhost:8080/policy/detail/550e8400-e29b-41d4-a716-446655440000' \
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
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "type": "PRIVACY_POLICY",
    "title": "Privacy Policy",
    "content": "This is our privacy policy...",
    "version": "1.0",
    "effectiveDate": "2023-01-01T00:00:00Z",
    "isActive": true,
    "createdAt": "2023-01-01T00:00:00Z",
    "updatedAt": "2023-01-01T00:00:00Z"
  }
}
```

#### Response Fields

Same as "Get Policy by Type" response fields.

---

### 4. Create Policy

**`POST /policy`**

Create a new policy with specified type, title, content, version, and effective date.

#### Headers

| Header | Value | Required |
|--------|-------|----------|
| `accept` | `application/json` | Yes |
| `Authorization` | `Bearer <access_token>` | Yes |
| `Content-Type` | `application/json` | Yes |

#### Request Body

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `type` | string | Yes | Policy type (PRIVACY_POLICY or TERMS_CONDITIONS) |
| `title` | string | Yes | Title of the policy |
| `content` | string | Yes | Full content of the policy |
| `version` | string | Yes | Version number of the policy |
| `effectiveDate` | string | Yes | ISO 8601 date when policy becomes effective |

#### Example Request

```bash
curl -X 'POST' \
  'http://localhost:8080/policy' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9...' \
  -H 'Content-Type: application/json' \
  -d '{
  "type": "PRIVACY_POLICY",
  "title": "Updated Privacy Policy",
  "content": "This is our updated privacy policy...",
  "version": "1.1",
  "effectiveDate": "2023-06-15T00:00:00Z"
}'
```

#### Example Response

```json
{
  "isSuccess": true,
  "statusCode": {
    "value": 201,
    "description": "Created"
  },
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440003",
    "type": "PRIVACY_POLICY",
    "title": "Updated Privacy Policy",
    "content": "This is our updated privacy policy...",
    "version": "1.1",
    "effectiveDate": "2023-06-15T00:00:00Z",
    "isActive": true
  }
}
```

#### Response Fields

| Field | Type | Description |
|-------|------|-------------|
| `data.id` | string | Unique identifier for the created policy |
| `data.type` | string | Policy type |
| `data.title` | string | Title of the policy |
| `data.content` | string | Full content of the policy |
| `data.version` | string | Version number of the policy |
| `data.effectiveDate` | string | ISO 8601 date when policy becomes effective |
| `data.isActive` | boolean | Whether the policy is currently active (defaults to true) |

---

### 5. Update Policy

**`PUT /policy/{id}`**

Update an existing policy by its ID. You can modify the title, content, version, and effective date.

#### Path Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `id` | string | Yes | Unique identifier of the policy to update |

#### Headers

| Header | Value | Required |
|--------|-------|----------|
| `accept` | `application/json` | Yes |
| `Authorization` | `Bearer <access_token>` | Yes |
| `Content-Type` | `application/json` | Yes |

#### Request Body

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `title` | string | No | New title for the policy |
| `content` | string | No | New content for the policy |
| `version` | string | No | New version number |
| `effectiveDate` | string | No | New effective date in ISO 8601 format |

#### Example Request

```bash
curl -X 'PUT' \
  'http://localhost:8080/policy/550e8400-e29b-41d4-a716-446655440003' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9...' \
  -H 'Content-Type: application/json' \
  -d '{
  "title": "Updated Privacy Policy v2",
  "content": "This is our further updated privacy policy...",
  "version": "1.2",
  "effectiveDate": "2023-07-01T00:00:00Z"
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
    "id": "550e8400-e29b-41d4-a716-446655440003",
    "type": "PRIVACY_POLICY",
    "title": "Updated Privacy Policy v2",
    "content": "This is our further updated privacy policy...",
    "version": "1.2",
    "effectiveDate": "2023-07-01T00:00:00Z",
    "isActive": true
  }
}
```

#### Response Fields

| Field | Type | Description |
|-------|------|-------------|
| `data.id` | string | Unique identifier of the updated policy |
| `data.type` | string | Policy type (unchanged) |
| `data.title` | string | Updated title of the policy |
| `data.content` | string | Updated content of the policy |
| `data.version` | string | Updated version number |
| `data.effectiveDate` | string | Updated effective date |
| `data.isActive` | boolean | Whether the policy is currently active |

---

### 6. Deactivate Policy

**`POST /policy/deactivate/{id}`**

Deactivate a policy by its ID. This sets the policy's isActive flag to false without deleting it.

#### Path Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `id` | string | Yes | Unique identifier of the policy to deactivate |

#### Headers

| Header | Value | Required |
|--------|-------|----------|
| `accept` | `application/json` | Yes |
| `Authorization` | `Bearer <access_token>` | Yes |

#### Example Request

```bash
curl -X 'POST' \
  'http://localhost:8080/policy/deactivate/550e8400-e29b-41d4-a716-446655440003' \
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
    "id": "550e8400-e29b-41d4-a716-446655440003",
    "type": "PRIVACY_POLICY",
    "title": "Updated Privacy Policy v2",
    "content": "This is our further updated privacy policy...",
    "version": "1.2",
    "effectiveDate": "2023-07-01T00:00:00Z",
    "isActive": false
  }
}
```

#### Response Fields

| Field | Type | Description |
|-------|------|-------------|
| `data.id` | string | Unique identifier of the deactivated policy |
| `data.type` | string | Policy type |
| `data.title` | string | Title of the policy |
| `data.content` | string | Content of the policy |
| `data.version` | string | Version number |
| `data.effectiveDate` | string | Effective date |
| `data.isActive` | boolean | Whether the policy is active (will be false) |

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
| `201` | Created - Policy successfully created |
| `400` | Bad Request - Invalid parameters or missing required fields |
| `401` | Unauthorized - Invalid or missing authentication |
| `403` | Forbidden - Insufficient privileges |
| `404` | Not Found - Policy not found |
| `409` | Conflict - Policy version conflict or duplicate policy |
| `500` | Internal Server Error - Server error |