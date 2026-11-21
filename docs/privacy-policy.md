# Privacy Policy

This documentation provides comprehensive details for the Privacy Policy API endpoints. The API supports creating, retrieving, updating, and managing privacy policies and terms of conditions within the platform. Policies can be versioned, activated/deactivated, and managed by administrators.

## Authentication

Most Policy endpoints require Bearer token authentication. Include the access token in the Authorization header:

```
Authorization: Bearer <your_access_token>
```

### Policy Endpoints

| Method | Endpoint | Description | Authentication Required |
|--------|----------|-------------|------------------------|
| `GET` | `/api/v1/policies/{policyType}` | Retrieve latest active policy by type | No |
| `POST` | `/api/v1/admin/policies` | Create a new policy or version | Yes (Admin) |
| `PUT` | `/api/v1/admin/policies/{id}` | Update an existing policy document | Yes (Admin) |
| `POST` | `/api/v1/admin/policies/deactivate/{id}` | Deactivate a policy document | Yes (Admin) |
| `GET` | `/api/v1/admin/policies/by-id/{id}` | Retrieve a policy document by ID | Yes (Admin) |
| `GET` | `/api/v1/admin/policies/{policyType}/history` | Retrieve all versions of a policy type | Yes (Admin) |

---

## Endpoint Details

### 1. Get Policy by Type

**`GET /api/v1/policies/{policyType}`**

Retrieve a list of all policies including Privacy Policies and Terms & Conditions.

#### Headers

| Header | Value | Required |
|--------|-------|----------|
| `accept` | `application/json` | Yes |

#### Request

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/policies/PRIVACY_POLICY' \
  -H 'accept: application/json'
```

#### Response

```json
[
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

---

### 2. Create Policy

**`POST /api/v1/admin/policies`**

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

#### Request

```bash
curl -X 'POST' \
  'http://localhost:8080/api/v1/admin/policies' \
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

#### Response

```json
{
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

---

### 3. Get Policy History

**`GET /api/v1/admin/policies/{type}/history`**

Get policy version history

#### Path Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `type` | string | Yes | Policy type (PRIVACY_POLICY or TERMS_CONDITIONS) |

#### Headers

| Header | Value | Required |
|--------|-------|----------|
| `accept` | `application/json` | Yes |
| `Authorization` | `Bearer <access_token>` | Yes |

#### Request

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/admin/policies/PRIVACY_POLICY/history' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9...'
```

#### Response

```json
{
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

---

## Policy Consent Endpoints

### Policy Consent Endpoints Table

| Method | Endpoint | Description | Authentication Required |
|--------|----------|-------------|------------------------|
| `POST` | `/api/v1/policy-consents/consent` | Create a new policy consent record | Yes |

---

### 4. Create Policy Consent

**`POST /api/v1/policy-consents/consent`**

Create a new consent record when a user agrees to a privacy policy. This endpoint captures the policy ID and user information for audit purposes.

#### Headers

| Header | Value | Required |
|--------|-------|----------|
| `Authorization` | `Bearer <access_token>` | Yes |
| `Content-Type` | `application/json` | Yes |

#### Request Body

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `policyId` | string | Yes | UUID of the policy being consented to |

#### Request

```bash
curl -X 'POST' \
  'http://localhost:8080/api/v1/policy-consents/consent' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9...' \
  -H 'Content-Type: application/json' \
  -d '{
  "policyId": "550e8400-e29b-41d4-a716-446655440000"
}'
```

#### Response

```json
{
    "id": "550e8400-e29b-41d4-a716-446655440002",
    "userId": "a67fd0cc-3d92-4259-bbd4-1e0ba49dece4",
    "policyId": "550e8400-e29b-41d4-a716-446655440000",
    "ipAddress": "127.0.0.1",
    "userAgent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36",
    "consentedAt": "2023-06-15T10:30:00Z"
  }
}
```

---

## Additional Admin Endpoints

### 5. Admin - Update an existing policy document

**`PUT /api/v1/admin/policies/{id}`**

Admin - Update an existing policy document

#### Request

```bash
curl -X 'PUT' \
  'http://localhost:8080/api/v1/admin/policies/6c5078d3-f8e3-4c88-9afe-48b5423c664f' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <admin_token>' \
  -H 'Content-Type: application/json' \
  -d '{
  "title": "Example",
  "content": "string",
  "version": "1.0.0",
  "effectiveDate": "2024-05-06T12:00:00",
  "isActive": true
}'
```

#### Response

```json
{
  "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "title": "Example",
  "type": "CUSTOMER",
  "content": "string",
  "version": "1.0.0",
  "effectiveDate": "2024-05-06T12:00:00",
  "isActive": true,
  "createdAt": "2024-05-06T12:00:00",
  "updatedAt": "2024-05-06T12:00:00"
}
```

---

### 6. Admin - Deactivate a policy document

**`POST /api/v1/admin/policies/deactivate/{id}`**

Admin - Deactivate a policy document

#### Request

```bash
curl -X 'POST' \
  'http://localhost:8080/api/v1/admin/policies/deactivate/6c5078d3-f8e3-4c88-9afe-48b5423c664f' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <admin_token>'
```

#### Response

```json
true
```

---

### 7. Admin - Retrieve a policy document by ID

**`GET /api/v1/admin/policies/by-id/{id}`**

Admin - Retrieve a policy document by ID

#### Request

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/admin/policies/by-id/6c5078d3-f8e3-4c88-9afe-48b5423c664f' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <admin_token>'
```

#### Response

```json
{
  "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
  "title": "Example",
  "type": "CUSTOMER",
  "content": "string",
  "version": "1.0.0",
  "effectiveDate": "2024-05-06T12:00:00",
  "isActive": true,
  "createdAt": "2024-05-06T12:00:00",
  "updatedAt": "2024-05-06T12:00:00"
}
```

---

### 8. Export all personal data held for the authenticated user

**`GET /api/v1/privacy/export`**

Export all personal data held for the authenticated user

#### Request

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/privacy/export' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <access_token>'
```

#### Response

```json
{
  "user": {
    "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
    "email": "jane@example.com",
    "isVerified": true,
    "userType": "CUSTOMER",
    "isActive": true,
    "mfaEnabled": false,
    "createdAt": "2024-05-06T12:00:00",
    "updatedAt": "2024-05-06T12:00:00"
  },
  "profile": {
    "userId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
    "image": "string",
    "firstName": "Jane",
    "lastName": "Doe",
    "mobile": "string",
    "faxNumber": "string",
    "streetAddress": "123 Main St",
    "city": "New York",
    "state": "PENDING",
    "country": "US",
    "identificationType": "CUSTOMER",
    "identificationNo": "string",
    "occupation": "string",
    "postCode": "SAVE10",
    "gender": "string",
    "dateOfBirth": "2024-05-06T12:00:00",
    "bio": "string",
    "isActive": true,
    "verified": true
  },
  "shippingAddresses": [
    {
      "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "userId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "firstName": "Jane",
      "lastName": "Doe",
      "email": "jane@example.com",
      "phoneNumber": "+15551234567",
      "streetAddress": "123 Main St",
      "city": "New York",
      "state": "PENDING",
      "country": "US",
      "zipCode": "SAVE10",
      "isDefault": true
    }
  ],
  "orders": [
    {
      "orderId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "orderNumber": "ORD-20240506-0001-A1B2C3D4",
      "userId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "shopId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "subTotal": "250.00",
      "shippingCost": "25.00",
      "taxAmount": "250.00",
      "discountAmount": "250.00",
      "total": "250.00",
      "currency": "USD",
      "status": "PENDING",
      "allowedActions": [
        "APPROVE"
      ],
      "nextStatuses": [
        "SHIPPED"
      ],
      "paymentStatus": "PENDING",
      "couponCode": "SAVE10",
      "paymentMethod": "CREDIT_CARD",
      "notes": "Looks good",
      "contactEmail": "jane@example.com",
      "shippingAddress": "123 Main St",
      "billingAddress": "123 Main St"
    }
  ],
  "payments": [
    {
      "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "orderId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "amount": "250.00",
      "currency": "USD",
      "status": "PENDING",
      "paymentMethod": "CREDIT_CARD",
      "transactionId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "createdAt": "2024-05-06T12:00:00",
      "provider": "MANUAL",
      "providerReference": "pi_123456789",
      "clientSecret": "pi_123_secret_456",
      "nextActionUrl": "https://example.com/next",
      "scaStatus": "NOT_REQUIRED",
      "providerStatus": "succeeded",
      "riskScore": 1,
      "riskDecision": "ALLOW",
      "avsStatus": "MATCH",
      "cvvStatus": "MATCH",
      "threeDsEnforced": true
    }
  ],
  "refunds": [
    {
      "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "orderItemId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "orderId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "orderNumber": "ORD-20240506-0001-A1B2C3D4",
      "shopId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "customerEmail": "jane@example.com",
      "productName": "Wireless Headphones",
      "userId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "reason": "Policy violation",
      "images": "https://example.com/image.jpg",
      "decisionReason": "Policy violation",
      "status": "PENDING",
      "currency": "USD",
      "quantity": 1,
      "refundAmount": "250.00",
      "shippingRefund": "25.00",
      "taxRefund": "25.00",
      "refundMethod": "ORIGINAL",
      "trackingNumber": "1Z999AA10123456784",
      "allowedActions": [
        "APPROVE"
      ]
    }
  ],
  "consents": [
    {
      "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "userId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "policyId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "consentDate": "2024-05-06T12:00:00",
      "ipAddress": "123 Main St",
      "userAgent": "string",
      "revokedAt": "2024-05-06T12:00:00",
      "policyVersion": "string",
      "policyContentHash": "string"
    }
  ],
  "reviews": [
    {
      "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "userId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "productId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "reviewText": "Looks good",
      "rating": 1,
      "title": "Example",
      "isVerifiedPurchase": true,
      "helpfulCount": 1,
      "notHelpfulCount": 1,
      "images": [
        "https://example.com/image.jpg"
      ],
      "status": "ACTIVE",
      "sellerReply": "Looks good",
      "sellerRepliedAt": "2024-05-06T12:00:00",
      "moderationReason": "Policy violation",
      "createdAt": "2024-05-06T12:00:00"
    }
  ],
  "notifications": [
    {
      "id": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "channel": "EMAIL",
      "type": "CUSTOMER",
      "title": "Example",
      "body": "Looks good",
      "resourceType": "CUSTOMER",
      "resourceId": "6c5078d3-f8e3-4c88-9afe-48b5423c664f",
      "isRead": true,
      "createdAt": "2024-05-06T12:00:00"
    }
  ],
  "additionalData": {},
  "exportedAt": "2024-05-06T12:00:00"
}
```

---

### 9. Erase/anonymize the authenticated account (password required)

**`POST /api/v1/privacy/erase`**

Erase/anonymize the authenticated account (password required)

#### Request

```bash
curl -X 'POST' \
  'http://localhost:8080/api/v1/privacy/erase' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer <access_token>' \
  -H 'Content-Type: application/json' \
  -d '{
  "password": "string"
}'
```

#### Response

```json
{
  "message": "Operation completed"
}
```

---

## Error Handling

This API follows industry-standard error handling patterns (Stripe, GitHub, OpenAI):

### Success Responses
- **HTTP status code indicates success** (200, 201, 204)
- **Response body contains data directly** (no wrapper object)
- No `isSuccess` or `statusCode` fields needed

### Error Responses

**Standard Error (400/401/403/404/500):**
```json
{
  "message": "Error description"
}
```

**Validation Error (400):**
```json
{
  "message": "Validation failed",
  "errors": [
    {"field": "email", "message": "Invalid email format"},
    {"field": "password", "message": "Password must be at least 8 characters"}
  ]
}
```

### Common Error Codes

| Status Code | Description | Example Message |
|-------------|-------------|-----------------|
| `400` | Bad Request | `"Invalid email or password"` |
| `401` | Unauthorized | `"Authentication required"` |
| `403` | Forbidden | `"Insufficient permissions"` |
| `404` | Not Found | `"Product not found"` |
| `409` | Conflict | `"User already exists with this email"` |
| `500` | Internal Server Error | `"Internal server error"` |

All error messages are centralized and consistent across all endpoints.

---

## Business Rules

### Consent Creation
- Policy ID must reference an existing policy in the system
- User consent is automatically linked to the authenticated user
- IP address and user agent are captured automatically from the request
- Duplicate consent records for the same policy may be prevented

### Data Privacy
- Consent records serve as legal documentation for compliance
- IP addresses and user agents are stored for audit purposes
- Consent timestamps use ISO 8601 format in UTC

## Policy Types

Common policy types supported by the system:

| Policy Type | Description |
|-------------|-------------|
| `PRIVACY_POLICY` | Privacy policy |
| `TERMS_CONDITIONS` | Terms and conditions |
| `REFUND_POLICY` | Refund policy |
| `COOKIE_POLICY` | Cookie usage |
| `DISCLAIMER` | Platform disclaimer |
| `EULA` | End-user license agreement |
| `SHIPPING_POLICY` | Shipping policy |

## GDPR Data-Subject Requests

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/v1/privacy/export` | Export the authenticated user's personal data |
| `POST` | `/api/v1/privacy/erase` | Erase/anonymize the account (body: `{"password":"..."}`) |

Orders and payments are retained in pseudonymous form for financial/legal obligations; direct identifiers, addresses, carts, wishlists, reviews, consents and notifications are removed or anonymized.
