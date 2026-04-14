# Authentication API

This documentation provides comprehensive details for the Authentication API endpoints. The API supports user registration, login, password management, token refresh, email verification, and account security features.

**Base URL:** `http://localhost:8080`

### Auth Endpoints

| Method | Endpoint | Description | Authentication Required |
|--------|----------|-------------|------------------------|
| `POST` | `/auth/login` | Authenticate user and receive access + refresh tokens | No |
| `POST` | `/auth/register` | Register a new user account | No |
| `GET` | `/auth/otp-verification` | Verify OTP for account activation | No |
| `POST` | `/auth/forget-password` | Request password reset verification code | No |
| `POST` | `/auth/reset-password` | Reset password using verification code | No |
| `POST` | `/auth/refresh-token` | Refresh access token using refresh token | No |
| `POST` | `/auth/logout` | Logout and revoke refresh token | Yes |
| `PUT` | `/auth/change-password` | Change password for authenticated user | Yes |
| `PUT` | `/auth/{userId}/change-user-type` | Change user's account type | Yes (Admin/Super Admin) |
| `PUT` | `/auth/{userId}/deactivate` | Deactivate user account | Yes (Admin/Super Admin) |
| `PUT` | `/auth/{userId}/activate` | Activate user account | Yes (Admin/Super Admin) |

---

## Endpoint Details

### 1. User Login

**`POST /auth/login`**

Authenticate a user and receive access + refresh tokens for subsequent API calls.

> **Security**: This endpoint is rate-limited to 5 requests per 10 minutes per IP address to prevent brute-force attacks. After 5 failed login attempts, the account will be locked for 30 minutes.

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

**Status: 200 OK**

```json
{
  "user": {
    "id": "ce563774-d3d5-442e-ad1a-b884bb0a53f0",
    "email": "customer@gmail.com",
    "userType": "customer"
  },
  "accessToken": "eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "expiresIn": 86400
}
```

#### Error Responses

**Invalid Credentials (401):**
```json
{
  "message": "Invalid email or password. 3 attempts remaining."
}
```

**Account Locked (400):**
```json
{
  "message": "Account locked due to too many failed login attempts. Try again in 30 minutes."
}
```

---

### 2. User Registration

**`POST /auth/register`**

Register a new user account. Users can register with the same email for different roles.

> **Security**: This endpoint is rate-limited to 5 requests per 10 minutes per IP address. Passwords must meet strength requirements: minimum 8 characters with uppercase, lowercase, digit, and special character.

#### Key Features
- Multiple user types supported (customer, seller, admin)
- Same email can be used for different user types
- Automatic email verification process
- Password strength validation enforced

#### Request Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `email` | string | Yes | User's email address |
| `password` | string | Yes | User's password (min 8 chars, uppercase, lowercase, digit, special char) |
| `userType` | string | Yes | Type of user (`customer`, `seller`, `admin`) |

#### Example Request

```bash
curl -X 'POST' \
  'http://localhost:8080/auth/register' \
  -H 'accept: application/json' \
  -H 'Content-Type: application/json' \
  -d '{
    "email": "piash@gmail.com",
    "password": "Str0ng!Pass",
    "userType": "admin"
  }'
```

#### Example Response

**Status: 201 Created**

```json
{
  "id": "f48ec4f9-5482-4a23-9e49-e69f97bd20a6",
  "email": "piash@gmail.com"
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

**Status: 200 OK**

```json
true
```

---

### 4. Forget Password

**`POST /auth/forget-password`**

Request a password reset verification code. The code will be sent to the user's email address.

> **Security**: This endpoint is rate-limited to 5 requests per 10 minutes per IP address.

#### Request Body

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `email` | string | Yes | User's email address |
| `userType` | string | Yes | Specify user type (`customer`, `seller`, `admin`) |

#### Example Request

```bash
curl -X 'POST' \
  'http://localhost:8080/auth/forget-password' \
  -H 'accept: application/json' \
  -H 'Content-Type: application/json' \
  -d '{
    "email": "piash@gmail.com",
    "userType": "customer"
  }'
```

#### Example Response

**Status: 200 OK**

```json
{
  "message": "Verification code sent to your email"
}
```

---

### 5. Reset Password

**`POST /auth/reset-password`**

Reset user password using the verification code sent via email.

> **Security**: This endpoint is rate-limited to 5 requests per 10 minutes per IP address. Passwords must meet strength requirements.

#### Request Body

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `email` | string | Yes | User's email address |
| `otp` | string | Yes | Verification code from email |
| `newPassword` | string | Yes | New password (min 8 chars, uppercase, lowercase, digit, special char) |
| `userType` | string | Yes | Specify user type if multiple accounts exist |

#### Example Request

```bash
curl -X 'POST' \
  'http://localhost:8080/auth/reset-password' \
  -H 'accept: application/json' \
  -H 'Content-Type: application/json' \
  -d '{
    "email": "piash@gmail.com",
    "otp": "9889",
    "newPassword": "Str0ng!NewPass",
    "userType": "customer"
  }'
```

#### Example Response

**Status: 200 OK**

```json
{
  "message": "Password reset successful"
}
```

---

### 6. Refresh Token

**`POST /auth/refresh-token`**

Refresh an expired access token using a valid refresh token. Returns a new access token and refresh token pair.

#### Request Body

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `refreshToken` | string | Yes | Refresh token received during login |

#### Example Request

```bash
curl -X 'POST' \
  'http://localhost:8080/auth/refresh-token' \
  -H 'accept: application/json' \
  -H 'Content-Type: application/json' \
  -d '{
    "refreshToken": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
  }'
```

#### Example Response

**Status: 200 OK**

```json
{
  "accessToken": "eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "b2c3d4e5-f6g7-8901-bcde-fg2345678901",
  "expiresIn": 86400
}
```

#### Error Responses

**Invalid/Expired Token (404):**
```json
{
  "message": "Invalid or expired refresh token"
}
```

---

### 7. Logout

**`POST /auth/logout`**

Logout the authenticated user and revoke the refresh token to prevent further token refreshes.

#### Headers

| Header | Value | Required |
|--------|-------|----------|
| `Authorization` | `Bearer <access_token>` | Yes |
| `Content-Type` | `application/json` | Yes |

#### Request Body

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `refreshToken` | string | No | Specific refresh token to revoke (if empty, all user tokens revoked) |

#### Example Request

```bash
curl -X 'POST' \
  'http://localhost:8080/auth/logout' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9...' \
  -H 'Content-Type: application/json' \
  -d '{
    "refreshToken": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
  }'
```

#### Example Response

**Status: 200 OK**

```json
{
  "message": "Logged out successfully"
}
```

---

### 8. Change Password

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

**Status: 200 OK**

```json
"Password has been changed"
```

---

### 9. Change User Type

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

**Status: 200 OK**

```json
"User type changed successfully to ADMIN"
```

---

### 10. Deactivate User

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
"User deactivated successfully"
```

---

### 11. Activate User

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
"User activated successfully"
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
    {"field": "password", "message": "Password must be at least 8 characters with uppercase, lowercase, digit, and special character"}
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
| `429` | Too Many Requests | `"Rate limit exceeded. Try again later."` |
| `500` | Internal Server Error | `"Internal server error"` |

All error messages are centralized and consistent across all endpoints.

---

## Security Features

### Rate Limiting
Auth endpoints (`login`, `register`, `forget-password`, `reset-password`) are rate-limited to **5 requests per 10 minutes** per IP address. Exceeding this limit returns a `429 Too Many Requests` response.

### Account Lockout
After **5 consecutive failed login attempts**, the account is automatically locked for **30 minutes**. The login response includes remaining attempts count until the lockout is triggered.

### Password Strength
All passwords must meet the following criteria:
- Minimum 8 characters
- At least one uppercase letter
- At least one lowercase letter
- At least one digit
- At least one special character

### Refresh Token Security
- Refresh tokens are hashed before storage (SHA-256)
- Tokens expire after 7 days
- Each token refresh automatically revokes the old token and issues a new pair
- Logout revokes all user refresh tokens

### Administrative User Management Guidelines

### Role Management
- Only Admin and Super Admin accounts can modify user types
- User type changes may affect access to different parts of the system
- Deactivated users cannot log in but their data remains intact
- Activation restores full account access and functionality

### Security Considerations
- Changing user types should be done carefully to maintain system security
- Deactivation should be used for temporary account suspension
- Regular monitoring of user type changes helps maintain security
