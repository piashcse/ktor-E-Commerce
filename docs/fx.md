# FX Rates

Checkout supports a currency per order. Non-USD checkout **fails closed** unless a stored
rate exists; rates are snapshotted onto `order.fx_rate` so refunds and payouts use the same rate.

## Admin endpoints

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/v1/admin/fx-rates` | List stored rates |
| `PUT` | `/api/v1/admin/fx-rates/{currency}` | Set/refresh a rate |

Rates are expressed as units of the target currency per 1 USD. Staleness is surfaced in the
admin listing so operators can refresh before peak traffic.
