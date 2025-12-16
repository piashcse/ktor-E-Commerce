# Privacy Policy Consents API

This documentation provides comprehensive details for the Privacy Policy Consents API endpoints. The API supports creating and retrieving user consent records for privacy policies and other policy types within the platform. Consents are tracked with metadata including IP address, user agent, and timestamp for audit purposes.

**Base URL:** `http://localhost:8080`

## Authentication

All Privacy Policy Consent endpoints require Bearer token authentication. Include the access token in the Authorization header:

```
Authorization: Bearer <your_access_token>
```

### Privacy Policy Consent Endpoints

| Method | Endpoint | Description | Authentication Required |
|--------|----------|-------------|------------------------|
| `POST` | `/policy/consent` | Create a new policy consent record | Yes |
| `GET` | `/policy-consents` | Retrieve all user consent records | Yes |
| `GET` | `/policy-consents/{policyType}` | Check if user has consented to specific policy type | Yes |

---

## Endpoint Details

### 1. Create Policy Consent

**`POST /policy/consent`**

Create a new consent record when a user agrees to a privacy policy or other policy type. This endpoint automatically captures metadata such as IP address and user agent for audit trails.

#### Headers

| Header | Value | Required |
|--------|-------|----------|
| `Authorization` | `Bearer <access_token>` | Yes |
| `Content-Type` | `application/json` | Yes |

#### Request Body

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `policyId` | string | Yes | UUID of the policy being consented to |

#### Example Request

```bash
curl -X 'POST' \
  'http://localhost:8080/policy/consent' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9...' \
  -H 'Content-Type: application/json' \
  -d '{
  "policyId": "550e8400-e29b-41d4-a716-446655440000"
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
    "id": "550e8400-e29b-41d4-a716-446655440002",
    "userId": "a67fd0cc-3d92-4259-bbd4-1e0ba49dece4",
    "policyId": "550e8400-e29b-41d4-a716-446655440000",
    "ipAddress": "127.0.0.1",
    "userAgent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36",
    "consentedAt": "2023-06-15T10:30:00Z"
  }
}
```

#### Response Fields

| Field | Type | Description |
|-------|------|-------------|
| `id` | string | Unique identifier for the consent record |
| `userId` | string | ID of the user who provided consent |
| `policyId` | string | UUID of the policy that was consented to |
| `ipAddress` | string | IP address from which consent was given |
| `userAgent` | string | Browser user agent string when consent was provided |
| `consentedAt` | string | ISO 8601 timestamp when consent was recorded |

---

### 2. Get User Consents

**`GET /user-consents`**

Retrieve all consent records for the authenticated user, including detailed policy information.

#### Headers

| Header | Value | Required |
|--------|-------|----------|
| `Authorization` | `Bearer <access_token>` | Yes |

#### Example Request

```bash
curl -X 'GET' \
  'http://localhost:8080/user-consents' \
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
      "id": "550e8400-e29b-41d4-a716-446655440002",
      "userId": "a67fd0cc-3d92-4259-bbd4-1e0ba49dece4",
      "policyId": "550e8400-e29b-41d4-a716-446655440000",
      "policyType": "PRIVACY_POLICY",
      "policyVersion": "1.0",
      "ipAddress": "127.0.0.1",
      "userAgent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36",
      "consentedAt": "2023-06-15T10:30:00Z"
    }
  ]
}
```

#### Response Fields

| Field | Type | Description |
|-------|------|-------------|
| `data` | array | Array of consent record objects |
| `data[].id` | string | Unique identifier for the consent record |
| `data[].userId` | string | ID of the user who provided consent |
| `data[].policyId` | string | UUID of the policy that was consented to |
| `data[].policyType` | string | Type of policy (e.g., "PRIVACY_POLICY") |
| `data[].policyVersion` | string | Version of the policy when consent was given |
| `data[].ipAddress` | string | IP address from which consent was given |
| `data[].userAgent` | string | Browser user agent string when consent was provided |
| `data[].consentedAt` | string | ISO 8601 timestamp when consent was recorded |

---

### 3. Check Policy Type Consent

**`GET /user-consents/{policyType}`**

Check if the authenticated user has provided consent for a specific policy type.

#### Path Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `policyType` | string | Yes | Type of policy to check (e.g., "PRIVACY_POLICY") |

#### Headers

| Header | Value | Required |
|--------|-------|----------|
| `Authorization` | `Bearer <access_token>` | Yes |

#### Example Request

```bash
curl -X 'GET' \
  'http://localhost:8080/user-consents/PRIVACY_POLICY' \
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
    "hasConsented": true
  }
}
```

#### Response Fields

| Field | Type | Description |
|-------|------|-------------|
| `data.hasConsented` | boolean | Whether the user has consented to the specified policy type |

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
| `201` | Created - Consent record created successfully |
| `400` | Bad Request - Invalid parameters or missing required fields |
| `401` | Unauthorized - Invalid or missing authentication |
| `403` | Forbidden - Insufficient privileges |
| `404` | Not Found - Policy or user not found |
| `409` | Conflict - Consent already exists for this policy |
| `500` | Internal Server Error - Server error |

---

## Business Rules

### Consent Creation
- Policy ID must reference an existing policy in the system
- User consent is automatically linked to the authenticated user
- IP address and user agent are captured automatically from the request
- Duplicate consent records for the same policy may be prevented

### Consent Retrieval
- Users can only access their own consent records
- Policy type filtering is case-sensitive
- Consent records include full audit trail information

### Data Privacy
- Consent records serve as legal documentation for compliance
- IP addresses and user agents are stored for audit purposes
- Consent timestamps use ISO 8601 format in UTC

---

## Policy Types

Common policy types supported by the system:

| Policy Type | Description |
|-------------|-------------|
| `PRIVACY_POLICY` | Privacy policy consent |
| `TERMS_OF_SERVICE` | Terms of service agreement |
| `COOKIE_POLICY` | Cookie usage consent |
| `MARKETING_CONSENT` | Marketing communication consent |