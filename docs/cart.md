# Cart

Manage the authenticated customer's shopping cart, update item quantities, and calculate cart checkout totals.

---

## Authentication

All Cart endpoints require **Bearer JWT access token** authentication for a registered `CUSTOMER` account:

```http
Authorization: Bearer <your_access_token>
```

---

## Cart Endpoints Overview

| Method | Endpoint | Description | Auth Required |
|:---|:---|:---|:---|
| <span class="method-badge post">POST</span> | `/api/v1/carts` | Add product or variant to cart | <span class="badge-req">Bearer JWT</span> |
| <span class="method-badge get">GET</span> | `/api/v1/carts` | Retrieve cart items (Paginated) | <span class="badge-req">Bearer JWT</span> |
| <span class="method-badge get">GET</span> | `/api/v1/carts/summary` | Retrieve cart summary with estimated tax & subtotal | <span class="badge-req">Bearer JWT</span> |
| <span class="method-badge put">PUT</span> | `/api/v1/carts/update` | Update product quantity in cart (0 removes item) | <span class="badge-req">Bearer JWT</span> |
| <span class="method-badge delete">DELETE</span> | `/api/v1/carts/remove` | Remove specific product from cart | <span class="badge-req">Bearer JWT</span> |
| <span class="method-badge delete">DELETE</span> | `/api/v1/carts/all` | Clear entire shopping cart | <span class="badge-req">Bearer JWT</span> |

---

## Endpoint Details

### 1. Add Product to Cart

<div class="endpoint-header">
  <span class="method-badge post">POST</span>
  <span class="endpoint-path">/api/v1/carts</span>
  <span class="auth-badge">Bearer Token Required</span>
</div>

Adds an item line to the authenticated user's cart. If the product has variants (e.g. Size, Color), `variantId` must be supplied.

#### Request Body Parameters

| Parameter | Type | Required | Description |
|:---|:---|:---|:---|
| `productId` | string | <span class="badge-req">Required</span> | UUID of the product to add |
| `quantity` | integer | <span class="badge-req">Required</span> | Quantity to add (must be positive, e.g. `>= 1`) |
| `variantId` | string | <span class="badge-opt">Conditional</span> | UUID of the selected variant. Mandatory if the product has variants. |

#### Request Examples

=== "cURL"
    ```bash
    curl -X POST 'http://localhost:8080/api/v1/carts' \
      -H 'Accept: application/json' \
      -H 'Content-Type: application/json' \
      -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9...' \
      -d '{
        "productId": "5b24d429-c981-47c8-9318-f4d61dd2c1a4",
        "quantity": 1
      }'
    ```

=== "Kotlin (Ktor Client)"
    ```kotlin
    val response = client.post("http://localhost:8080/api/v1/carts") {
        bearerAuth(accessToken)
        contentType(ContentType.Application.Json)
        setBody(CartRequest(productId = "5b24d429-c981-47c8-9318-f4d61dd2c1a4", quantity = 1))
    }
    ```

=== "TypeScript (Fetch)"
    ```typescript
    const res = await fetch("http://localhost:8080/api/v1/carts", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "Authorization": `Bearer ${accessToken}`
      },
      body: JSON.stringify({
        productId: "5b24d429-c981-47c8-9318-f4d61dd2c1a4",
        quantity: 1
      })
    });
    ```

#### Response

=== "201 Created"
    ```json
    {
      "productId": "5b24d429-c981-47c8-9318-f4d61dd2c1a4",
      "quantity": 1
    }
    ```

=== "400 Bad Request"
    ```json
    {
      "message": "variantId is required for products with variants"
    }
    ```

---

### 2. Get Cart Items

<div class="endpoint-header">
  <span class="method-badge get">GET</span>
  <span class="endpoint-path">/api/v1/carts</span>
  <span class="auth-badge">Bearer Token Required</span>
</div>

Retrieves all line items currently saved in the customer's shopping cart.

#### Query Parameters

| Parameter | Type | Required | Description |
|:---|:---|:---|:---|
| `page` / `offset` | integer | <span class="badge-opt">Optional</span> | Page number (default: 1) or offset (default: 0) |
| `perPage` / `limit` | integer | <span class="badge-opt">Optional</span> | Items per page (default: 10, max: 100) |

#### Request Examples

=== "cURL"
    ```bash
    curl -X GET 'http://localhost:8080/api/v1/carts?page=1&perPage=10' \
      -H 'Accept: application/json' \
      -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9...'
    ```

#### Response

=== "200 OK"
    ```json
    [
      {
        "productId": "71b26dd9-b4b5-4f87-a84d-c8daa506018a",
        "quantity": 3,
        "product": {
          "id": "71b26dd9-b4b5-4f87-a84d-c8daa506018a",
          "categoryId": "58f5c085-d04a-47de-beab-1d476b6ce432",
          "productName": "Smart Watch",
          "productCode": "SW-001",
          "productQuantity": 5,
          "productDetail": "Xiaomi Smart Watch",
          "price": 10.0,
          "discountPrice": 0.0,
          "status": "ACTIVE"
        }
      }
    ]
    ```

---

### 3. Update Cart Item Quantity

<div class="endpoint-header">
  <span class="method-badge put">PUT</span>
  <span class="endpoint-path">/api/v1/carts/update</span>
  <span class="auth-badge">Bearer Token Required</span>
</div>

Sets the absolute quantity of an item in the cart. Setting `quantity: 0` automatically removes the item line.

#### Request Body Parameters

| Parameter | Type | Required | Description |
|:---|:---|:---|:---|
| `productId` | string | <span class="badge-req">Required</span> | UUID of the product |
| `quantity` | integer | <span class="badge-req">Required</span> | New absolute quantity (`>= 0`) |
| `variantId` | string | <span class="badge-opt">Optional</span> | Variant identifier if updating a variant line |

#### Request Examples

=== "cURL"
    ```bash
    curl -X PUT 'http://localhost:8080/api/v1/carts/update' \
      -H 'Accept: application/json' \
      -H 'Content-Type: application/json' \
      -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9...' \
      -d '{
        "productId": "5b24d429-c981-47c8-9318-f4d61dd2c1a4",
        "quantity": 2
      }'
    ```

=== "Kotlin (Ktor Client)"
    ```kotlin
    val response = client.put("http://localhost:8080/api/v1/carts/update") {
        bearerAuth(accessToken)
        contentType(ContentType.Application.Json)
        setBody(CartRequest(productId = "5b24d429-c981-47c8-9318-f4d61dd2c1a4", quantity = 2))
    }
    ```

#### Response

=== "200 OK"
    ```json
    {
      "productId": "5b24d429-c981-47c8-9318-f4d61dd2c1a4",
      "quantity": 2,
      "product": {
        "id": "5b24d429-c981-47c8-9318-f4d61dd2c1a4",
        "productName": "Polo T-Shirt",
        "price": 100.0,
        "discountPrice": 0.0
      }
    }
    ```

---

### 4. Remove Product from Cart

<div class="endpoint-header">
  <span class="method-badge delete">DELETE</span>
  <span class="endpoint-path">/api/v1/carts/remove</span>
  <span class="auth-badge">Bearer Token Required</span>
</div>

Removes an item line completely from the customer's cart.

#### Query Parameters

| Parameter | Type | Required | Description |
|:---|:---|:---|:---|
| `productId` | string | <span class="badge-req">Required</span> | UUID of the product to remove |
| `variantId` | string | <span class="badge-opt">Optional</span> | UUID of the variant line to remove |

#### Request Examples

=== "cURL"
    ```bash
    curl -X DELETE 'http://localhost:8080/api/v1/carts/remove?productId=71b26dd9-b4b5-4f87-a84d-c8daa506018a' \
      -H 'Accept: application/json' \
      -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9...'
    ```

#### Response

=== "200 OK"
    ```json
    {
      "id": "71b26dd9-b4b5-4f87-a84d-c8daa506018a",
      "productName": "Smart Watch"
    }
    ```

---

### 5. Clear Cart

<div class="endpoint-header">
  <span class="method-badge delete">DELETE</span>
  <span class="endpoint-path">/api/v1/carts/all</span>
  <span class="auth-badge">Bearer Token Required</span>
</div>

Empties all lines from the customer's shopping cart.

#### Request Examples

=== "cURL"
    ```bash
    curl -X DELETE 'http://localhost:8080/api/v1/carts/all' \
      -H 'Accept: application/json' \
      -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9...'
    ```

#### Response

=== "200 OK"
    ```json
    {
      "message": "Cart cleared successfully"
    }
    ```

---

### 6. Get Cart Summary

<div class="endpoint-header">
  <span class="method-badge get">GET</span>
  <span class="endpoint-path">/api/v1/carts/summary</span>
  <span class="auth-badge">Bearer Token Required</span>
</div>

Calculates variant-aware subtotals, item counts, available stock, and estimated taxation.

#### Request Examples

=== "cURL"
    ```bash
    curl -X GET 'http://localhost:8080/api/v1/carts/summary' \
      -H 'Accept: application/json' \
      -H 'Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9...'
    ```

#### Response

=== "200 OK"
    ```json
    {
      "items": [
        {
          "productId": "71b26dd9-b4b5-4f87-a84d-c8daa506018a",
          "productName": "Smart Watch",
          "price": 10.0,
          "quantity": 3,
          "image": "https://example.com/image1.jpg",
          "stockQuantity": 50,
          "shopId": "shop-uuid-1",
          "shopName": "Electronics Shop"
        }
      ],
      "subtotal": 30.0,
      "estimatedTax": 3.0,
      "itemCount": 1
    }
    ```

---

## Error Handling

!!! tip "Standard Error Responses"
    For details on error schemas, validation failure formats, and HTTP status codes (`400`, `401`, `404`), see the **[Error Handling & Status Codes Guide](errors.md)**.
