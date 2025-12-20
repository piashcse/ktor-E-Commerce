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
| `PUT` | `/auth/{userId}/change-user-type` | Change user's account type | Yes (Admin/Super Admin) |
| `PUT` | `/auth/{userId}/deactivate` | Deactivate user account | Yes (Admin/Super Admin) |
| `PUT` | `/auth/{userId}/activate` | Activate user account | Yes (Admin/Super Admin) |

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

### 7. Change User Type

**`PUT /auth/{userId}/change-user-type`**

Change an existing user's account type. This endpoint is available to administrators.

#### Path Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `userId` | string | Yes | Unique identifier of the user to update |

#### Query Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `userType` | string | Yes | New user type to assign (`CUSTOMER`, `SELLER`, `ADMIN`, `SUPER_ADMIN`) |

#### Headers

| Header | Value | Required |
|--------|-------|----------|
| `accept` | `application/json` | Yes |
| `Authorization` | `Bearer <access_token>` | Yes (Admin/Super Admin) |

#### Example Request

```bash
curl -X 'PUT' \
  'http://localhost:8080/auth/a67fd0cc-3d92-4259-bbd4-1e0ba49dece4/change-user-type?userType=ADMIN' \
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
  "data": "User type changed successfully to ADMIN"
}
```

---

### 8. Deactivate User

**`PUT /auth/{userId}/deactivate`**

Deactivate a user account, preventing the user from logging in or accessing the system. This operation can be reversed with the activate endpoint.

#### Path Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `userId` | string | Yes | Unique identifier of the user to deactivate |

#### Headers

| Header | Value | Required |
|--------|-------|----------|
| `accept` | `application/json` | Yes |
| `Authorization` | `Bearer <access_token>` | Yes (Admin/Super Admin) |

#### Example Request

```bash
curl -X 'PUT' \
  'http://localhost:8080/auth/a67fd0cc-3d92-4259-bbd4-1e0ba49dece4/deactivate' \
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
  "data": "User deactivated successfully"
}
```

---

### 9. Activate User

**`PUT /auth/{userId}/activate`**

Activate a previously deactivated user account, allowing the user to log in and access the system again.

#### Path Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `userId` | string | Yes | Unique identifier of the user to activate |

#### Headers

| Header | Value | Required |
|--------|-------|----------|
| `accept` | `application/json` | Yes |
| `Authorization` | `Bearer <access_token>` | Yes (Admin/Super Admin) |

#### Example Request

```bash
curl -X 'PUT' \
  'http://localhost:8080/auth/a67fd0cc-3d92-4259-bbd4-1e0ba49dece4/activate' \
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
  "data": "User activated successfully"
}
```

---

## Administrative Auth Endpoints

### Administrative Auth Endpoints Table

| Method | Endpoint | Description | Authentication Required |
|--------|----------|-------------|------------------------|
| `PUT` | `/auth/{userId}/change-user-type` | Change user's account type | Yes (Admin/Super Admin) |
| `PUT` | `/auth/{userId}/deactivate` | Deactivate user account | Yes (Admin/Super Admin) |
| `PUT` | `/auth/{userId}/activate` | Activate user account | Yes (Admin/Super Admin) |

---

### 7. Change User Type

**`PUT /auth/{userId}/change-user-type`**

Change an existing user's account type. This endpoint allows administrators to upgrade or downgrade user roles.

#### Path Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `userId` | string | Yes | Unique identifier of the user to update |

#### Query Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `userType` | string | Yes | New user type to assign (`CUSTOMER`, `SELLER`, `ADMIN`, `SUPER_ADMIN`) |

#### Headers

| Header | Value | Required |
|--------|-------|----------|
| `accept` | `application/json` | Yes |
| `Authorization` | `Bearer <access_token>` | Yes (Admin/Super Admin) |

#### Example Request

```bash
curl -X 'PUT' \
  'http://localhost:8080/auth/a67fd0cc-3d92-4259-bbd4-1e0ba49dece4/change-user-type?userType=ADMIN' \
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
  "data": "User type changed successfully to ADMIN"
}
```

#### Response Fields

| Field | Type | Description |
|-------|------|-------------|
| `data` | string | Message confirming the user type change |

---

### 8. Deactivate User

**`PUT /auth/{userId}/deactivate`**

Deactivate a user account, preventing the user from logging in or accessing the system. This operation can be reversed with the activate endpoint.

#### Path Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `userId` | string | Yes | Unique identifier of the user to deactivate |

#### Headers

| Header | Value | Required |
|--------|-------|----------|
| `accept` | `application/json` | Yes |
| `Authorization` | `Bearer <access_token>` | Yes (Admin/Super Admin) |

#### Example Request

```bash
curl -X 'PUT' \
  'http://localhost:8080/auth/a67fd0cc-3d92-4259-bbd4-1e0ba49dece4/deactivate' \
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
  "data": "User deactivated successfully"
}
```

#### Response Fields

| Field | Type | Description |
|-------|------|-------------|
| `data` | string | Message confirming the deactivation |

---

### 9. Activate User

**`PUT /auth/{userId}/activate`**

Activate a previously deactivated user account, allowing the user to log in and access the system again.

#### Path Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `userId` | string | Yes | Unique identifier of the user to activate |

#### Headers

| Header | Value | Required |
|--------|-------|----------|
| `accept` | `application/json` | Yes |
| `Authorization` | `Bearer <access_token>` | Yes (Admin/Super Admin) |

#### Example Request

```bash
curl -X 'PUT' \
  'http://localhost:8080/auth/a67fd0cc-3d92-4259-bbd4-1e0ba49dece4/activate' \
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
  "data": "User activated successfully"
}
```

#### Response Fields

| Field | Type | Description |
|-------|------|-------------|
| `data` | string | Message confirming the activation |

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
| `403` | Forbidden - Insufficient privileges |
| `404` | Not Found - Resource not found |
| `500` | Internal Server Error - Server error |

---

## Administrative User Management Guidelines

### Role Management
- Only Admin and Super Admin accounts can modify user types
- User type changes may affect access to different parts of the system
- Deactivated users cannot log in but their data remains intact
- Activation restores full account access and functionality

### Security Considerations
- Changing user types should be done carefully to maintain system security
- Deactivation should be used for temporary account suspension
- Regular monitoring of user type changes helps maintain security
