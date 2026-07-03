package com.piashcse.mapper

import com.piashcse.database.entities.OrderDAO
import com.piashcse.database.entities.OrderItemDAO
import com.piashcse.model.response.OrderItemResponse
import com.piashcse.model.response.OrderResponse

fun OrderDAO.toOrderResponse(items: List<OrderItemResponse>? = null) = OrderResponse(
    orderId = id.value,
    orderNumber = orderNumber,
    userId = userId.value,
    shopId = shopId?.value,
    subTotal = subTotal.toPlainString(),
    shippingCost = shippingCost.toPlainString(),
    taxAmount = taxAmount.toPlainString(),
    discountAmount = discountAmount.toPlainString(),
    total = total.toPlainString(),
    currency = currency,
    status = status,
    paymentStatus = paymentStatus,
    couponCode = couponCode,
    paymentMethod = paymentMethod,
    notes = notes,
    shippingAddress = shippingAddress,
    billingAddress = billingAddress,
    shippingMethod = shippingMethod,
    shippingDate = shippingDate,
    deliveredDate = deliveredDate,
    canceledDate = canceledDate,
    completedDate = completedDate,
    createdAt = createdAt,
    updatedAt = updatedAt,
    items = items ?: OrderItemDAO.itemsForOrder(id).map { it.toOrderItemResponse() },
)

fun OrderItemDAO.toOrderItemResponse() = OrderItemResponse(
    productId = productId.value,
    productName = productName,
    quantity = quantity,
    price = price.toPlainString(),
    discountAmount = discountAmount.toPlainString(),
    taxAmount = taxAmount.toPlainString(),
    total = total.toPlainString(),
    sku = sku,
)
