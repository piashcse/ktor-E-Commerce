# Refund Request API

This documentation provides comprehensive details for the Refund Request API endpoints.

**Base URL:** `http://localhost:8080`

## Authentication

All Refund Request endpoints require Bearer token authentication.

### Refund Request Endpoints

| Method | Endpoint | Description | Authentication Required |
|--------|----------|-------------|------------------------|
| `POST` | `/api/v1/refund-requests/{orderId}` | Create a refund request | Yes (Customer) |
| `GET` | `/api/v1/refund-requests/{id}` | Get refund request details | Yes |
| `GET` | `/api/v1/seller/refund-requests/order/{orderId}` | Get refunds for an order (Seller) | Yes (Seller) |
| `PUT` | `/api/v1/seller/refund-requests/{id}/status` | Update refund status (Seller) | Yes (Seller) |
| `POST` | `/api/v1/refund-requests/{id}/ship` | Mark refund as shipped | Yes (Customer) |
| `GET` | `/api/v1/admin/refund-requests/order/{orderId}` | Get refunds for an order (Admin) | Yes (Admin) |
| `PUT` | `/api/v1/admin/refund-requests/{id}/status` | Update refund status (Admin) | Yes (Admin) |

---

## Endpoint Details

### 1. Update Refund Status (Seller)

**`PUT /api/v1/seller/refund-requests/{id}/status`**

Update the status of a refund request (APPROVED, REJECTED, etc.).

#### Example Request

```bash
curl -X 'PUT' \
  'http://localhost:8080/api/v1/seller/refund-requests/refund-uuid-123/status' \
  -H 'Authorization: Bearer <seller_token>' \
  -H 'Content-Type: application/json' \
  -d '{
  "status": "APPROVED",
  "refundAmount": 50.0,
  "refundMethod": "ORIGINAL"
}'
```

---

## Error Handling

| Status Code | Description |
|-------------|-------------|
| `400` | Bad Request |
| `401` | Unauthorized |
| `403` | Forbidden (User does not own the order/shop) |
| `404` | Not Found |
