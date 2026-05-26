# Coupon Management

The Coupon module provides a flexible way to manage discounts and promotional offers. It supports both fixed amount and percentage-based discounts with various validation rules.

## Coupon Types
- **FIXED**: A fixed discount amount (e.g., $10 off).
- **PERCENTAGE**: A percentage-based discount (e.g., 20% off).

## Features
- **Minimum Order Amount**: Coupons can require a minimum subtotal to be applicable.
- **Max Discount Amount**: For percentage coupons, you can cap the maximum discount value.
- **Usage Limits**: Track how many times a coupon has been used and set a maximum limit.
- **Expiry Dates**: Define start and end dates for promotional campaigns.

## Customer API

### Check Coupon Validity
`GET /api/v1/coupons/{code}`

Returns the coupon details if found and active.

**Response:**
```json
{
  "id": "uuid",
  "code": "SUMMER20",
  "discountType": "PERCENTAGE",
  "discountValue": 20.0,
  "minOrderAmount": 50.0,
  "maxDiscountAmount": 10.0,
  "startDate": "2024-01-01T00:00:00",
  "endDate": "2024-12-31T23:59:59",
  "usageLimit": 100,
  "usageCount": 5,
  "isActive": true
}
```

## Admin API

All admin endpoints require `ADMIN` role.

### Create Coupon
`POST /api/v1/admin/coupons`

### List Coupons
`GET /api/v1/admin/coupons?limit=10&offset=0`

### Update Coupon
`PUT /api/v1/admin/coupons/{id}`

### Delete Coupon
`DELETE /api/v1/admin/coupons/{id}`
