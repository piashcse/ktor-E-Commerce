# Profile API

This documentation provides comprehensive details for the Profile API endpoints. The API supports retrieving user profile information, updating profile details, and uploading profile images for authenticated users.

**Base URL:** `http://localhost:8080`

## Authentication

All Profile endpoints require Bearer token authentication. Include the access token in the Authorization header:

```
Authorization: Bearer <your_access_token>
```

### Profile Endpoints

| Method | Endpoint | Description | Authentication Required |
|--------|----------|-------------|------------------------|
| `GET` | `/profile` | Retrieve user profile information | Yes |
| `PUT` | `/profile` | Update user profile information | Yes |
| `POST` | `/profile/image-upload` | Upload user profile image | Yes |

---

## Endpoint Details

### 1. Get User Profile

**`GET /profile`**

Retrieve the profile information for the authenticated user.

#### Headers

| Header | Value | Required |
|--------|-------|----------|
| `Authorization` | `Bearer <access_token>` | Yes |

#### Example Request

```bash
curl -X 'GET' \
  'http://localhost:8080/profile' \
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
    "userId": "707ac264-be2e-4e89-b6d3-7a49b14263d2",
    "firstName": "Mehedi ",
    "lastName": "Hassan",
    "mobile": "01812353930",
    "faxNumber": "454",
    "streetAddress": "Dhaka",
    "city": "Dhaka",
    "postCode": "1205",
    "gender": "Male"
  }
}
```

#### Response Fields

| Field | Type | Description |
|-------|------|-------------|
| `userId` | string | Unique identifier for the user |
| `firstName` | string | User's first name |
| `lastName` | string | User's last name |
| `mobile` | string | User's mobile phone number |
| `faxNumber` | string | User's fax number |
| `streetAddress` | string | User's street address |
| `city` | string | User's city |
| `postCode` | string | User's postal code |
| `gender` | string | User's gender |

---

### 2. Update User Profile

**`PUT /profile`**

Update profile information for the authenticated user.

#### Query Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `firstName` | string | No | User's first name |
| `lastName` | string | No | User's last name |
| `mobile` | string | No | User's mobile phone number |
| `faxNumber` | string | No | User's fax number |
| `streetAddress` | string | No | User's street address |
| `city` | string | No | User's city |
| `postCode` | string | No | User's postal code |
| `gender` | string | No | User's gender |

#### Headers

| Header | Value | Required |
|--------|-------|----------|
| `Authorization` | `Bearer <access_token>` | Yes |

#### Example Request

```bash
curl -X 'PUT' \
  'http://localhost:8080/profile?firstName=Mehedi&lastName=Hassan&mobile=01812353930&faxNumber=454&streetAddress=Dhaka&city=Dhaka&postCode=1205&gender=Male' \
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
    "userId": "707ac264-be2e-4e89-b6d3-7a49b14263d2",
    "firstName": "Mehedi",
    "lastName": "Hassan",
    "mobile": "01812353930",
    "faxNumber": "454",
    "streetAddress": "Dhaka",
    "city": "Dhaka",
    "postCode": "1205",
    "gender": "Male"
  }
}
```

---

### 3. Upload Profile Image

**`POST /profile/image-upload`**

Upload a profile image for the authenticated user. The image will be processed and stored with a unique filename.

#### Request Body

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `image` | file | Yes | Image file (multipart/form-data) |

#### Headers

| Header | Value | Required |
|--------|-------|----------|
| `Authorization` | `Bearer <access_token>` | Yes |
| `Content-Type` | `multipart/form-data` | Yes |

#### Supported Image Formats
- JPEG (.jpg, .jpeg)
- PNG (.png)
- GIF (.gif)

#### Example Request

```bash
curl -X 'POST' \
  'http://localhost:8080/profile/image-upload' \
  -H 'accept: application/json' \
  -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9...' \
  -H 'Content-Type: multipart/form-data' \
  -F 'image=@profile.jpg;type=image/jpeg'
```

#### Example Response

```json
{
  "isSuccess": true,
  "statusCode": {
    "value": 200,
    "description": "OK"
  },
  "data": "73b21d27-466e-45c6-bc2b-0480eb4db2d2.jpg"
}
```

#### Response Fields

| Field | Type | Description |
|-------|------|-------------|
| `data` | string | Unique filename of the uploaded image |

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
| `400` | Bad Request - Invalid parameters or file format |
| `401` | Unauthorized - Invalid or missing authentication |
| `413` | Payload Too Large - File size exceeds limit |
| `415` | Unsupported Media Type - Invalid file format |
| `500` | Internal Server Error - Server error |
