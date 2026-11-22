-- One live refund request per order item. Concurrency in RMA completion and
-- refund creation could otherwise insert two overlapping requests.
CREATE UNIQUE INDEX IF NOT EXISTS refund_request_one_live_per_item
  ON refund_request (order_item_id)
  WHERE status IN ('PENDING', 'APPROVED', 'SHIPPED');
