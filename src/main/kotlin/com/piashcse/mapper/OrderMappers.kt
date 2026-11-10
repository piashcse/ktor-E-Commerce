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
        contactEmail = contactEmail,
        shippingAddress = shippingAddress,
        billingAddress = billingAddress,
        shippingMethod = shippingMethod,
        trackingNumber = trackingNumber,
        shippingDate = shippingDate,
        deliveredDate = deliveredDate,
        canceledDate = canceledDate,
        completedDate = completedDate,
        createdAt = createdAt,
        updatedAt = updatedAt,
        items = items ?: OrderItemDAO.itemsForOrder(id).map { it.toOrderItemResponse() },
        businessTaxId = businessTaxId,
        taxExempt = taxExempt,
    )

fun OrderItemDAO.toOrderItemResponse() =
    OrderItemResponse(
        id = id.value,
        productId = productId.value,
        productName = productName,
        quantity = quantity,
        price = Money.str(price),
        discountAmount = Money.str(discountAmount),
        taxAmount = Money.str(taxAmount),
        total = Money.str(total),
        sku = sku,
        variantId = variantId?.value,
        variantName = variantName,
        taxRate = taxRate.toPlainString(),
        taxInclusive = taxInclusive,
        taxClassId = taxClassId?.value,
    )
