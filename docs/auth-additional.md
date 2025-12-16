# Additional Authentication API

This documentation provides comprehensive details for additional Authentication API endpoints. The API supports administrative user management including changing user types, deactivating, and activating user accounts.

**Base URL:** `http://localhost:8080`

## Authentication

Most additional auth endpoints require admin or super admin privileges. Include the appropriate access token in the Authorization header:

```
Authorization: Bearer <your_admin_access_token>
```

### Additional Auth Endpoints

| Method | Endpoint | Description | Authentication Required |
|--------|----------|-------------|------------------------|
| `PUT` | `/auth/{userId}/change-user-type` | Change user's account type | Yes (Admin/Super Admin) |
| `PUT` | `/auth/{userId}/deactivate` | Deactivate user account | Yes (Admin/Super Admin) |
| `PUT` | `/auth/{userId}/activate` | Activate user account | Yes (Admin/Super Admin) |

---

## Endpoint Details

### 1. Change User Type

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

### 2. Deactivate User

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

### 3. Activate User

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
| `400` | Bad Request - Invalid parameters or user type |
| `401` | Unauthorized - Invalid or missing authentication |
| `403` | Forbidden - Insufficient privileges (not admin/super admin) |
| `404` | Not Found - User not found |
| `409` | Conflict - User type change not allowed |
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