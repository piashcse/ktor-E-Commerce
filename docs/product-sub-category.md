# Product Sub Category

Create, update, list and remove product subcategories linked to parent product categories.

---

## Authentication

- **Public**: `GET /api/v1/product-subcategories` requires no authentication.
- **Admin**: All mutating endpoints (`POST`, `PUT`, `DELETE`) require an authenticated `ADMIN` account with `catalog:manage` permission:

```http
Authorization: Bearer <admin_access_token>
```

---

## Product Sub Category Endpoints

| Method | Endpoint | Description | Auth Required |
|:---|:---|:---|:---|
| <span class="method-badge get">GET</span> | `/api/v1/product-subcategories` | Retrieve subcategories for a category | <span class="auth-badge public">Public</span> |
| <span class="method-badge post">POST</span> | `/api/v1/admin/product-subcategories` | Create a new product subcategory | <span class="badge-req">Admin + catalog:manage</span> |
| <span class="method-badge put">PUT</span> | `/api/v1/admin/product-subcategories/{id}` | Update an existing subcategory name | <span class="badge-req">Admin + catalog:manage</span> |
| <span class="method-badge delete">DELETE</span> | `/api/v1/admin/product-subcategories/{id}` | Delete a product subcategory | <span class="badge-req">Admin + catalog:manage</span> |

---

## Endpoint Details

### 1. Get Product Sub Categories

<div class="endpoint-header">
  <span class="method-badge get">GET</span>
  <span class="endpoint-path">/api/v1/product-subcategories</span>
  <span class="auth-badge public">Public / No Auth Required</span>
</div>

Retrieves all subcategories associated with a mandatory parent category UUID.

#### Query Parameters

| Parameter | Type | Required | Description |
|:---|:---|:---|:---|
| `categoryId` | string | <span class="badge-req">Required</span> | UUID of the parent product category |
| `page` / `offset` | integer | <span class="badge-opt">Optional</span> | Page number (default: 1) or offset (default: 0) |
| `perPage` / `limit` | integer | <span class="badge-opt">Optional</span> | Items per page (default: 20) |

#### Request Examples

=== "cURL"
    ```bash
    curl -X GET 'http://localhost:8080/api/v1/product-subcategories?categoryId=b8ccc13f-e118-4540-8e9e-5eaa8028cb4f&page=1&perPage=20' \
      -H 'Accept: application/json'
    ```

=== "TypeScript (Fetch)"
    ```typescript
    const res = await fetch("http://localhost:8080/api/v1/product-subcategories?categoryId=b8ccc13f-e118-4540-8e9e-5eaa8028cb4f");
    const subCategories = await res.json();
    ```

#### Response

=== "200 OK"
    ```json
    [
      {
        "id": "751cef10-f98a-4ecc-ae03-4173830a626d",
        "categoryId": "b8ccc13f-e118-4540-8e9e-5eaa8028cb4f",
        "name": "Paper"
      }
    ]
    ```

=== "400 Bad Request (Missing categoryId)"
    ```json
    {
      "message": "Missing required query parameter: categoryId"
    }
    ```

---

### 2. Create Product Sub Category

<div class="endpoint-header">
  <span class="method-badge post">POST</span>
  <span class="endpoint-path">/api/v1/admin/product-subcategories</span>
  <span class="auth-badge">Admin Token Required</span>
</div>

Creates a new subcategory linked to an existing parent category.

#### Request Body Parameters

| Parameter | Type | Required | Description |
|:---|:---|:---|:---|
| `categoryId` | string | <span class="badge-req">Required</span> | UUID of the parent category |
| `name` | string | <span class="badge-req">Required</span> | Subcategory display title |

#### Request Examples

=== "cURL"
    ```bash
    curl -X POST 'http://localhost:8080/api/v1/admin/product-subcategories' \
      -H 'Accept: application/json' \
      -H 'Content-Type: application/json' \
      -H 'Authorization: Bearer <admin_access_token>' \
      -d '{
        "categoryId": "b8ccc13f-e118-4540-8e9e-5eaa8028cb4f",
        "name": "Notebooks"
      }'
    ```

#### Response

=== "201 Created"
    ```json
    {
      "id": "751cef10-f98a-4ecc-ae03-4173830a626d",
      "categoryId": "b8ccc13f-e118-4540-8e9e-5eaa8028cb4f",
      "name": "Notebooks"
    }
    ```

---

### 3. Update Product Sub Category

<div class="endpoint-header">
  <span class="method-badge put">PUT</span>
  <span class="endpoint-path">/api/v1/admin/product-subcategories/{id}</span>
  <span class="auth-badge">Admin Token Required</span>
</div>

Updates the name of an existing product subcategory. The new name can be passed as a JSON body parameter or URL query parameter.

#### Path & Query Parameters

| Parameter | Type | Required | Description |
|:---|:---|:---|:---|
| `id` | string (path) | <span class="badge-req">Required</span> | UUID of the subcategory to update |
| `name` | string (query/body) | <span class="badge-req">Required</span> | Updated name |

#### Request Examples

=== "cURL"
    ```bash
    curl -X PUT 'http://localhost:8080/api/v1/admin/product-subcategories/751cef10-f98a-4ecc-ae03-4173830a626d?name=Premium+Paper' \
      -H 'Accept: application/json' \
      -H 'Authorization: Bearer <admin_access_token>'
    ```

#### Response

=== "200 OK"
    ```json
    {
      "id": "751cef10-f98a-4ecc-ae03-4173830a626d",
      "categoryId": "b8ccc13f-e118-4540-8e9e-5eaa8028cb4f",
      "name": "Premium Paper"
    }
    ```

---

### 4. Delete Product Sub Category

<div class="endpoint-header">
  <span class="method-badge delete">DELETE</span>
  <span class="endpoint-path">/api/v1/admin/product-subcategories/{id}</span>
  <span class="auth-badge">Admin Token Required</span>
</div>

Permanently removes a product subcategory.

#### Request Examples

=== "cURL"
    ```bash
    curl -X DELETE 'http://localhost:8080/api/v1/admin/product-subcategories/751cef10-f98a-4ecc-ae03-4173830a626d' \
      -H 'Accept: application/json' \
      -H 'Authorization: Bearer <admin_access_token>'
    ```

#### Response

=== "200 OK"
    ```json
    {
      "message": "Subcategory deleted successfully"
    }
    ```

---

## Error Handling

!!! tip "Standard Error Responses"
    For error payload structures, common status codes (`400`, `401`, `403`, `404`), and validation failure schemas, see the **[Error Handling & Status Codes Guide](errors.md)**.
