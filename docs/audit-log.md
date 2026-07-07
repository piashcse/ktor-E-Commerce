# Audit Log API

This documentation provides comprehensive details for the Audit Log API endpoints. The API provides a centralized, immutable record of all administrative actions for compliance monitoring, security auditing, and operational troubleshooting.

**Base URL:** `http://localhost:8080`

## Authentication

All Audit Log endpoints require `ADMIN` role.

### Audit Log Endpoints

| Method | Endpoint | Description | Authentication Required |
|--------|----------|-------------|------------------------|
| `GET` | `/api/v1/admin/audit-logs` | List audit logs | Yes (Admin) |
| `GET` | `/api/v1/admin/audit-logs/{id}` | Get audit log by ID | Yes (Admin) |

---

## Endpoint Details

### 1. List Audit Logs

**`GET /api/v1/admin/audit-logs`**

Returns paginated audit log entries with optional filters.

#### Query Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `actorId` | string | No | Filter by actor user ID |
| `action` | string | No | Filter by action type (e.g., `APPROVE_SHOP`, `DEACTIVATE_USER`) |
| `resourceType` | string | No | Filter by resource type (e.g., `shop`, `user`, `product`) |
| `resourceId` | string | No | Filter by specific resource ID |
| `outcome` | string | No | Filter by outcome (`SUCCESS`, `FAILURE`) |
| `limit` | int | No | Max results (default: 20, max: 100) |
| `offset` | int | No | Pagination offset |

#### Example Request

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/admin/audit-logs?action=APPROVE_SHOP&limit=20&offset=0' \
  -H 'Authorization: Bearer <admin_token>'
```

#### Example Response

```json
{
  "data": [
    {
      "id": "uuid",
      "actorId": "user-uuid",
      "actorEmail": "admin@example.com",
      "actorRole": "admin",
      "action": "APPROVE_SHOP",
      "resourceType": "shop",
      "resourceId": "shop-uuid",
      "details": "Shop approved by admin",
      "ipAddress": "192.168.1.1",
      "userAgent": "Mozilla/5.0...",
      "outcome": "SUCCESS",
      "executedAt": "2024-07-07T10:30:00",
      "createdAt": "2024-07-07T10:30:00"
    }
  ],
  "metadata": {
    "totalCount": 150,
    "limit": 20,
    "offset": 0
  }
}
```

---

### 2. Get Audit Log by ID

**`GET /api/v1/admin/audit-logs/{id}`**

Returns a single audit log entry.

#### Path Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `id` | string | Yes | Unique identifier of the audit log entry |

#### Example Request

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/admin/audit-logs/audit-log-uuid-123' \
  -H 'Authorization: Bearer <admin_token>'
```

#### Example Response

```json
{
  "id": "uuid",
  "actorId": "user-uuid",
  "actorEmail": "admin@example.com",
  "actorRole": "admin",
  "action": "APPROVE_SHOP",
  "resourceType": "shop",
  "resourceId": "shop-uuid",
  "details": "Shop approved by admin",
  "ipAddress": "192.168.1.1",
  "userAgent": "Mozilla/5.0...",
  "outcome": "SUCCESS",
  "executedAt": "2024-07-07T10:30:00",
  "createdAt": "2024-07-07T10:30:00"
}
```

---

## Error Handling

| Status Code | Description |
|-------------|-------------|
| `400` | Bad Request |
| `401` | Unauthorized |
| `403` | Forbidden |
| `404` | Not Found |
