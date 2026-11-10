# RMA / Returns & Exchanges

Return merchandise authorization (RMA) covers physical returns and exchanges for delivered, paid order items.

## Customer endpoints

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/v1/rma-requests` | Create a return/exchange request |
| `GET` | `/api/v1/rma-requests` | List your RMA requests |
| `POST` | `/api/v1/rma-requests/{id}/ship` | Mark an approved RMA as shipped back |
| `POST` | `/api/v1/rma-requests/{id}/cancel` | Cancel a pending/approved RMA |

Create body:

```json
{
  "orderId": "order-uuid",
  "orderItemId": "item-uuid",
  "quantity": 1,
  "type": "RETURN",
  "reason": "Item arrived damaged"
}
```

## Seller/Admin endpoints

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/v1/seller/rma-requests` | Seller queue |
| `PUT` | `/api/v1/seller/rma-requests/{id}/status` | Approve/reject/receive/complete |
| `GET` | `/api/v1/admin/rma-requests` | Admin queue (optional `status`) |
| `PUT` | `/api/v1/admin/rma-requests/{id}/status` | Admin status update |

Status update body:

```json
{
  "status": "COMPLETED",
  "resolution": "REFUND",
  "replacementOrderId": null,
  "note": "Returned item received"
}
```

- `REFUND` creates/links a normal refund request for the returned quantity.
- `REPLACEMENT` requires `replacementOrderId` belonging to the same customer and shop; returned stock is restored.

## Lifecycle

`REQUESTED → APPROVED → IN_TRANSIT → RECEIVED → COMPLETED`, or `REQUESTED/APPROVED → CANCELLED`, or `REQUESTED → REJECTED`.
