# Inventory API

This documentation provides comprehensive details for the Inventory API endpoints. The API supports managing inventory records for products.

**Base URL:** `http://localhost:8080`

## Authentication

All Inventory endpoints require Bearer token authentication for sellers and admins.

### Inventory Endpoints

| Method | Endpoint | Description | Authentication Required |
|--------|----------|-------------|------------------------|
| `POST` | `/api/v1/seller/inventories` | Create a new inventory record | Yes (Seller) |
| `PUT` | `/api/v1/seller/inventories/stock/{productId}` | Update product stock quantity | Yes (Seller) |
| `GET` | `/api/v1/seller/inventories/product/{productId}` | Retrieve inventory item details by product ID | Yes (Seller) |
| `GET` | `/api/v1/seller/inventories/shop/{shopId}` | Retrieve all inventory items for a shop | Yes (Seller) |
| `GET` | `/api/v1/seller/inventories/low-stock` | Retrieve items with stock below a threshold | Yes (Seller) |

---

## Endpoint Details

### 1. Update Product Stock (Seller)

**`PUT /api/v1/seller/inventories/stock/{productId}`**

Update the stock quantity for a specific product using atomic operations (`add`, `subtract`, `set`).

#### Example Request

```bash
curl -X 'PUT' \
  'http://localhost:8080/api/v1/seller/inventories/stock/cbd630f6-bf9f-48ad-ac51-f806807d99fd?quantity=50&operation=add' \
  -H 'Authorization: Bearer <seller_token>'
```

#### Example Response

```json
{
    "id": "cbd630f6-bf9f-48ad-ac51-f806807d99fd",
    "stockQuantity": 250,
    "status": "IN_STOCK"
}
```

---

### 2. Get Low Stock Records

**`GET /api/v1/seller/inventories/low-stock`**

Retrieve inventory records that have fallen below the minimum stock level.

#### Example Request

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/seller/inventories/low-stock' \
  -H 'Authorization: Bearer <seller_token>'
```

---

## Error Handling

| Status Code | Description |
|-------------|-------------|
| `400` | Bad Request |
| `401` | Unauthorized |
| `403` | Forbidden (User does not own the product) |
| `404` | Not Found |