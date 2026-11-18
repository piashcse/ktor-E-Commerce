package com.piashcse.mapper

import com.piashcse.constants.OrderStatus
import com.piashcse.database.entities.OrderDAO
import com.piashcse.database.entities.OrderItemDAO
import com.piashcse.feature.i18n.I18nService
import com.piashcse.model.response.OrderItemResponse
import com.piashcse.model.response.OrderResponse
import com.piashcse.model.response.PublicOrderItem
import com.piashcse.model.response.PublicOrderTrackingResponse
import com.piashcse.utils.common.DateTimes.toUtcString
import com.piashcse.utils.common.Money

private val i18n = I18nService()

fun OrderDAO.toOrderResponse(
    items: List<OrderItemResponse>? = null,
    includeSensitive: Boolean = true,
    forCustomer: Boolean = false,
) = OrderResponse(
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
    allowedActions =
        if (forCustomer) {
            when (status) {
                OrderStatus.PENDING, OrderStatus.CONFIRMED -> listOf("CANCEL")
                OrderStatus.DELIVERED -> listOf("RECEIVE")
                else -> emptyList()
            }
        } else {
            when (status) {
                OrderStatus.PENDING -> listOf("CONFIRM", "CANCEL")
                OrderStatus.CONFIRMED -> listOf("PAY", "CANCEL")
                OrderStatus.PAID -> listOf("SHIP")
                OrderStatus.SHIPPED -> listOf("DELIVER")
                OrderStatus.DELIVERED -> listOf("RECEIVE")
                OrderStatus.CANCELED, OrderStatus.RECEIVED -> emptyList()
            }
        },
    nextStatuses =
        OrderStatus.entries
            .filter { OrderStatus.canTransitionTo(status, it) }
            .filter { !forCustomer || it in listOf(OrderStatus.CANCELED, OrderStatus.RECEIVED) }
            .map { it.name },
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
    walletDiscount = Money.str(walletDiscount),
    giftCardDiscount = Money.str(giftCardDiscount),
    giftCardCode = giftCardCode.takeIf { includeSensitive },
    locale = locale,
    formattedTotal = i18n.formatMoney(total, currency, locale),
    attributionSource = attributionSource,
    attributionMedium = attributionMedium,
    attributionCampaign = attributionCampaign,
    fulfillmentType = fulfillmentType,
    pickupWarehouseId = pickupWarehouseId?.value,
    pickupCode = pickupCode.takeIf { includeSensitive },
    pickupStatus = pickupStatus,
    pickedUpAt = pickedUpAt,
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
        isDigital = isDigital,
    )

/** Public tracking projection that never leaks customer identity or address. */
fun OrderResponse.toPublicTrackingResponse() =
    PublicOrderTrackingResponse(
        orderNumber = orderNumber,
        status = status,
        paymentStatus = paymentStatus,
        total = total,
        currency = currency,
        itemCount = items?.sumOf { it.quantity } ?: 0,
        items = items.orEmpty().map { PublicOrderItem(it.productName, it.variantName, it.quantity, it.total.orEmpty()) },
        trackingNumber = trackingNumber,
        shippingDate = shippingDate?.toUtcString(),
        deliveredDate = deliveredDate?.toUtcString(),
        canceledDate = canceledDate?.toUtcString(),
        completedDate = completedDate?.toUtcString(),
        fulfillmentType = fulfillmentType,
        pickupStatus = pickupStatus,
    )
