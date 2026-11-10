# Public Shop Discovery

Public endpoints for browsing shops. No authentication is required. Shop management and admin
approval lives in [Shop API](shop.md).

**Base URL:** `http://localhost:8080`

## Endpoints

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/v1/shops/{id}` | Retrieve a shop by ID (only `APPROVED` shops are public) |
| `GET` | `/api/v1/shops/public` | List public shops with filters and pagination |
| `GET` | `/api/v1/shops/category/{categoryId}` | List public shops in a category |
| `GET` | `/api/v1/shops/featured` | List featured shops |

## Filters

`GET /api/v1/shops/public` accepts:

| Parameter | Type | Description |
|---|---|---|
| `search` | string | Match shop name/description |
| `categoryId` | string | Filter by shop category |
| `limit` / `offset` | number | Offset pagination (or `page`/`perPage`) |

```bash
curl -X 'GET' \
  'http://localhost:8080/api/v1/shops/public?categoryId=9c95c44c-3767-4ca2-9486-e28e390b3741&page=1&perPage=20'
```

Only shops with `status = APPROVED` are returned. Pending/suspended shops return `404` from the
public detail endpoint and are excluded from all public lists.
