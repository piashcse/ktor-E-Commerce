# Authentication API

This documentation provides comprehensive details for the Authentication API endpoints. The API supports user registration, login, password management, and email verification functionality with support for multiple user types (customer, seller, admin).

**Base URL:** `http://localhost:8080`

### Auth Endpoints

| Method | Endpoint | Description | Authentication Required |
|--------|----------|-------------|------------------------|
| `POST` | `/auth/login` | Authenticate user and receive access token | No |
| `POST` | `/auth/register` | Register a new user account | No |
| `GET` | `/auth/otp-verification` | Verify OTP for account activation | No |
| `GET` | `/auth/forget-password` | Request password reset verification code | No |
| `GET` | `/auth/reset-password` | Reset password using verification code | No |
| `PUT` | `/auth/change-password` | Change password for authenticated user | Yes |

---

## Endpoint Details

### 1. User Login

**`POST /auth/login`**

Authenticate a user and receive an access token for subsequent API calls.

#### Request Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `email` | string | Yes | User's email address |
| `password` | string | Yes | User's password |
| `userType` | string | Yes | Type of user (`customer`, `seller`, `admin`) |

#### Example Request

```bash
curl -X 'POST' \
  'http://localhost:8080/auth/login' \
  -H 'accept: application/json' \
  -H 'Content-Type: application/json' \
  -d '{
    "email": "customer@gmail.com",
    "password": "p1234",
    "userType": "customer"
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
    "user": {
      "id": "ce563774-d3d5-442e-ad1a-b884bb0a53f0",
      "email": "customer@gmail.com",
      "userType": "customer"
    },
    "accessToken": "eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9..."
  }
}
```

#### Response Fields

| Field | Type | Description |
|-------|------|-------------|
| `isSuccess` | boolean | Indicates if the request was successful |
| `statusCode` | object | HTTP status code and description |
| `data.user` | object | User information |
| `data.accessToken` | string | JWT token for authentication |

---

### 2. User Registration

**`POST /auth/register`**

Register a new user account. Users can register with the same email for different roles.

#### Key Features
- Multiple user types supported (customer, seller, admin)
- Same email can be used for different user types
- Automatic email verification process

#### Request Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `email` | string | Yes | User's email address |
| `password` | string | Yes | User's password |
| `userType` | string | Yes | Type of user (`customer`, `seller`, `admin`) |

#### Example Request

```bash
curl -X 'POST' \
  'http://localhost:8080/auth/register' \
  -H 'accept: application/json' \
  -H 'Content-Type: application/json' \
  -d '{
    "email": "piash@gmail.com",
    "password": "p1234",
    "userType": "admin"
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
    "id": "f48ec4f9-5482-4a23-9e49-e69f97bd20a6",
    "email": "piash@gmail.com"
  }
}
```

---

### 3. OTP Verification

**`GET /auth/otp-verification`**

Verify the OTP (One-Time Password) sent to user's email during registration.

#### Query Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `userId` | string | Yes | User ID received during registration |
| `otp` | string | Yes | OTP code sent to user's email |

#### Example Request

```bash
curl -X 'GET' \
  'http://localhost:8080/auth/otp-verification?userId=3842e19b-2608-40f8-98bd-6a6b43939fec&otp=560674d' \
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
  "data": true
}
```

---

### 4. Forget Password

**`GET /auth/forget-password`**

Request a password reset verification code. The code will be sent to the user's email address.

#### Query Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `email` | string | Yes | User's email address |
| `userType` | string | No | Specify user type if multiple accounts exist with same email |

#### Example Request

```bash
curl -X 'GET' \
  'http://localhost:8080/auth/forget-password?email=piash@gmail.com&userType=customer' \
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
  "data": "verification code sent to piash@gmail.com"
}
```

---

### 5. Reset Password

**`GET /auth/reset-password`**

Reset user password using the verification code sent via email.

#### Query Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `email` | string | Yes | User's email address |
| `otp` | string | Yes | Verification code from email |
| `newPassword` | string | Yes | New password |
| `userType` | string | No | Specify user type if multiple accounts exist |

#### Example Request

```bash
curl -X 'GET' \
  'http://localhost:8080/auth/reset-password?email=piash599%40gmail.com&otp=9889&newPassword=p1234&userType=customer' \
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
  "data": "Password change successful"
}
```

---

### 6. Change Password

**`PUT /auth/change-password`**

Change password for an authenticated user. Requires valid access token.

#### Query Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `oldPassword` | string | Yes | Current password |
| `newPassword` | string | Yes | New password |

#### Headers

| Header | Value | Required |
|--------|-------|----------|
| `Authorization` | `Bearer <access_token>` | Yes |

#### Example Request

```bash
curl -X 'PUT' \
  'http://localhost:8080/auth/change-password?oldPassword=p1234&newPassword=newp1234' \
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
  "data": "Password has been changed"
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
| `400` | Bad Request - Invalid parameters |
| `401` | Unauthorized - Invalid or missing authentication |
| `404` | Not Found - Resource not found |
| `500` | Internal Server Error - Server error |
