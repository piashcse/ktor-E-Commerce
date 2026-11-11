package com.piashcse.mapper

import com.piashcse.database.entities.OrderDAO
import com.piashcse.database.entities.OrderItemDAO
import com.piashcse.feature.i18n.I18nService
import com.piashcse.model.response.OrderItemResponse
import com.piashcse.model.response.OrderResponse
import com.piashcse.utils.common.Money

private val i18n = I18nService()

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
        walletDiscount = Money.str(walletDiscount),
        giftCardDiscount = Money.str(giftCardDiscount),
        giftCardCode = giftCardCode,
        locale = locale,
        formattedTotal = i18n.formatMoney(total, currency, locale),
        attributionSource = attributionSource,
        attributionMedium = attributionMedium,
        attributionCampaign = attributionCampaign,
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
