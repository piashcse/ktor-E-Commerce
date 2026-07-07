# Audit Log

The Audit Log module provides a centralized, immutable record of all administrative actions performed on the platform. It enables compliance monitoring, security auditing, and operational troubleshooting.

## Features
- **Action Tracking**: Records every admin action with actor identity, action type, and target resource.
- **Rich Context**: Captures actor email, role, IP address, user agent, and outcome for full audit trail.
- **Filterable Queries**: Search audit logs by actor, action type, resource, or outcome.
- **Immutable Records**: Logs are append-only and cannot be modified or deleted.

## Admin API

All audit log endpoints require `ADMIN` role.

### List Audit Logs
`GET /api/v1/admin/audit-logs?actorId=uuid&action=APPROVE_SHOP&resourceType=shop&outcome=SUCCESS&limit=20&offset=0`

Returns paginated audit log entries with optional filters.

**Query Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| `actorId` | string | Filter by actor user ID |
| `action` | string | Filter by action type (e.g., `APPROVE_SHOP`, `DEACTIVATE_USER`) |
| `resourceType` | string | Filter by resource type (e.g., `shop`, `user`, `product`) |
| `resourceId` | string | Filter by specific resource ID |
| `outcome` | string | Filter by outcome (`SUCCESS`, `FAILURE`) |
| `limit` | int | Max results (default: 20, max: 100) |
| `offset` | int | Pagination offset |

**Response:**
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

### Get Audit Log by ID
`GET /api/v1/admin/audit-logs/{id}`

Returns a single audit log entry.

**Response:**
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

## Usage in Services

Services can log admin actions by injecting `AuditLogService`:

```kotlin
auditLogService.log(
    actorId = userId,
    actorEmail = email,
    actorRole = "admin",
    action = "APPROVE_SHOP",
    resourceType = "shop",
    resourceId = shopId,
    details = "Shop approved",
    ipAddress = call.request.origin.remoteHost,
)
```

### Common Action Types
| Action | Resource Type | Description |
|--------|---------------|-------------|
| `APPROVE_SHOP` | shop | Shop approval |
| `REJECT_SHOP` | shop | Shop rejection |
| `SUSPEND_SHOP` | shop | Shop suspension |
| `ACTIVATE_SHOP` | shop | Shop activation |
| `DEACTIVATE_USER` | user | User deactivation |
| `ACTIVATE_USER` | user | User activation |
| `CHANGE_USER_TYPE` | user | User role change |
| `CREATE_COUPON` | coupon | Coupon creation |
| `UPDATE_COUPON` | coupon | Coupon update |
| `DELETE_COUPON` | coupon | Coupon deletion |
| `DELETE_PRODUCT` | product | Product deletion |
