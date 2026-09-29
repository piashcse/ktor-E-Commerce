package com.piashcse.mapper

import com.piashcse.database.entities.OrderDAO
import com.piashcse.database.entities.OrderItemDAO
import com.piashcse.model.response.OrderItemResponse
import com.piashcse.model.response.OrderResponse
import com.piashcse.utils.common.Money

fun OrderDAO.toOrderResponse(items: List<OrderItemResponse>? = null) =
    OrderResponse(
        orderId = id.value,
        orderNumber = orderNumber,
        userId = userId.value,
        shopId = shopId?.value,
        subTotal = Money.str(subTotal),
        shippingCost = Money.str(shippingCost),
        taxAmount = Money.str(taxAmount),
        discountAmount = Money.str(discountAmount),
        total = Money.str(total),
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

fun OrderItemDAO.toOrderItemResponse() =
    OrderItemResponse(
        productId = productId.value,
        productName = productName,
        quantity = quantity,
        price = Money.str(price),
        discountAmount = Money.str(discountAmount),
        taxAmount = Money.str(taxAmount),
        total = Money.str(total),
        sku = sku,
    )
