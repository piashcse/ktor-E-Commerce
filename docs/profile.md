# User Profile

Manage the authenticated user's account profile details and avatar image.

---

## Authentication

All endpoints in the Profile module require a valid **Bearer JWT access token** passed in the `Authorization` header:

```http
Authorization: Bearer <your_access_token>
```

---

## Profile Endpoints Overview

| Method | Endpoint | Description | Auth Required |
|:---|:---|:---|:---|
| <span class="method-badge get">GET</span> | `/api/v1/profile` | Retrieve user profile information | <span class="badge-req">Bearer JWT</span> |
| <span class="method-badge put">PUT</span> | `/api/v1/profile` | Update user profile information | <span class="badge-req">Bearer JWT</span> |
| <span class="method-badge post">POST</span> | `/api/v1/profile/image-upload` | Upload profile image (multipart) | <span class="badge-req">Bearer JWT</span> |

---

## Endpoint Details

### 1. Get User Profile

<div class="endpoint-header">
  <span class="method-badge get">GET</span>
  <span class="endpoint-path">/api/v1/profile</span>
  <span class="auth-badge">Bearer Token Required</span>
</div>

Retrieves the currently authenticated user's complete profile record.

#### Headers

| Header | Value | Required | Description |
|:---|:---|:---|:---|
| `Authorization` | `Bearer <access_token>` | <span class="badge-req">Required</span> | Valid user access token |
| `Accept` | `application/json` | <span class="badge-opt">Optional</span> | Expected response format |

#### Request Examples

=== "cURL"
    ```bash
    curl -X GET 'http://localhost:8080/api/v1/profile' \
      -H 'Accept: application/json' \
      -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9...'
    ```

=== "Kotlin (Ktor Client)"
    ```kotlin
    val response = client.get("http://localhost:8080/api/v1/profile") {
        header(HttpHeaders.Authorization, "Bearer $accessToken")
        accept(ContentType.Application.Json)
    }
    val profile: UserProfile = response.body()
    ```

=== "TypeScript (Fetch)"
    ```typescript
    const response = await fetch("http://localhost:8080/api/v1/profile", {
      headers: {
        "Accept": "application/json",
        "Authorization": `Bearer ${accessToken}`
      }
    });
    const profile = await response.json();
    ```

#### Response

=== "200 OK"
    ```json
    {
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
    ```

=== "401 Unauthorized"
    ```json
    {
      "message": "Authentication required"
    }
    ```

---

### 2. Update User Profile

<div class="endpoint-header">
  <span class="method-badge put">PUT</span>
  <span class="endpoint-path">/api/v1/profile</span>
  <span class="auth-badge">Bearer Token Required</span>
</div>

Updates profile information for the authenticated account.

#### Query / Form Parameters

| Parameter | Type | Required | Description |
|:---|:---|:---|:---|
| `firstName` | string | <span class="badge-opt">Optional</span> | User's given name |
| `lastName` | string | <span class="badge-opt">Optional</span> | User's family name |
| `mobile` | string | <span class="badge-opt">Optional</span> | User's mobile phone number |
| `faxNumber` | string | <span class="badge-opt">Optional</span> | User's fax contact number |
| `streetAddress` | string | <span class="badge-opt">Optional</span> | Street address line |
| `city` | string | <span class="badge-opt">Optional</span> | City or municipality |
| `postCode` | string | <span class="badge-opt">Optional</span> | Postal / ZIP code |
| `gender` | string | <span class="badge-opt">Optional</span> | Gender identity (`Male`, `Female`, `Other`) |

#### Headers

| Header | Value | Required | Description |
|:---|:---|:---|:---|
| `Authorization` | `Bearer <access_token>` | <span class="badge-req">Required</span> | Valid user access token |

#### Request Examples

=== "cURL"
    ```bash
    curl -X PUT 'http://localhost:8080/api/v1/profile?firstName=Mehedi&lastName=Hassan&mobile=01812353930&streetAddress=Dhaka&city=Dhaka&postCode=1205&gender=Male' \
      -H 'Accept: application/json' \
      -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9...'
    ```

=== "Kotlin (Ktor Client)"
    ```kotlin
    val response = client.put("http://localhost:8080/api/v1/profile") {
        header(HttpHeaders.Authorization, "Bearer $accessToken")
        parameter("firstName", "Mehedi")
        parameter("lastName", "Hassan")
        parameter("mobile", "01812353930")
        parameter("city", "Dhaka")
        parameter("postCode", "1205")
        parameter("gender", "Male")
    }
    val updatedProfile: UserProfile = response.body()
    ```

=== "TypeScript (Fetch)"
    ```typescript
    const params = new URLSearchParams({
      firstName: "Mehedi",
      lastName: "Hassan",
      mobile: "01812353930",
      city: "Dhaka",
      postCode: "1205",
      gender: "Male"
    });

    const response = await fetch(`http://localhost:8080/api/v1/profile?${params.toString()}`, {
      method: "PUT",
      headers: {
        "Authorization": `Bearer ${accessToken}`,
        "Accept": "application/json"
      }
    });
    const updatedProfile = await response.json();
    ```

#### Response

=== "200 OK"
    ```json
    {
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
    ```

=== "400 Bad Request"
    ```json
    {
      "message": "Validation failed",
      "errors": [
        { "field": "mobile", "message": "Invalid mobile number format" }
      ]
    }
    ```

---

### 3. Upload Profile Image

<div class="endpoint-header">
  <span class="method-badge post">POST</span>
  <span class="endpoint-path">/api/v1/profile/image-upload</span>
  <span class="auth-badge">Bearer Token Required</span>
</div>

Uploads a new avatar image for the authenticated user.

#### Multipart Form Data

| Field | Type | Required | Description |
|:---|:---|:---|:---|
| `image` | binary file | <span class="badge-req">Required</span> | Supported formats: `image/jpeg`, `image/png`, `image/webp` (`.jpg`, `.jpeg`, `.png`, `.webp`). Maximum size: **5 MB**. |

#### Headers

| Header | Value | Required | Description |
|:---|:---|:---|:---|
| `Authorization` | `Bearer <access_token>` | <span class="badge-req">Required</span> | Valid user access token |
| `Content-Type` | `multipart/form-data` | <span class="badge-req">Required</span> | Multipart boundary header |

#### Request Examples

=== "cURL"
    ```bash
    curl -X POST 'http://localhost:8080/api/v1/profile/image-upload' \
      -H 'Authorization: Bearer <access_token>' \
      -H 'Content-Type: multipart/form-data' \
      -F 'image=@profile.jpg;type=image/jpeg'
    ```

=== "Kotlin (Ktor Client)"
    ```kotlin
    val response = client.submitFormWithBinaryData(
        url = "http://localhost:8080/api/v1/profile/image-upload",
        formData = formData {
            append("image", fileBytes, Headers.build {
                append(HttpHeaders.ContentType, "image/jpeg")
                append(HttpHeaders.ContentDisposition, "filename=\"profile.jpg\"")
            })
        }
    ) {
        header(HttpHeaders.Authorization, "Bearer $accessToken")
    }
    val uploadedUrl: String = response.body()
    ```

#### Response

=== "200 OK"
    ```json
    "/uploads/profile-images/73b21d27-466e-45c6-bc2b-0480eb4db2d2.jpg"
    ```

=== "400 Bad Request (Exceeds Size / Unsupported Format)"
    ```json
    {
      "message": "File size exceeds the 5MB limit or invalid image type"
    }
    ```

---

## Error Handling

!!! tip "Centralized Error Handling Architecture"
    All profile endpoints return standardized error structures and HTTP status codes. For the complete reference of status codes (`400`, `401`, `403`, `404`, `500`), validation error formats, and retry guidelines, see the **[Error Handling & Status Codes Guide](errors.md)**.
