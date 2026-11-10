package com.piashcse.feature.order

import com.piashcse.constants.Message
import com.piashcse.constants.OrderStatus
import com.piashcse.constants.UserType
import com.piashcse.utils.validator.ForbiddenException

/**
 * Domain rule for who may move an order into a given status.
 *
 * Kept out of the HTTP handler so the same rule is reusable by admin tooling,
 * background jobs and future service-layer callers.
 */
object OrderStatusPolicy {
    private val sellerTransitions = setOf(OrderStatus.CONFIRMED, OrderStatus.SHIPPED, OrderStatus.DELIVERED)
    private val customerTransitions = setOf(OrderStatus.CANCELED, OrderStatus.RECEIVED)

    fun assertCanSet(
        userType: UserType,
        target: OrderStatus,
    ) {
        val allowed =
            when (userType) {
                UserType.SELLER -> sellerTransitions
                UserType.CUSTOMER -> customerTransitions
                else -> emptySet()
            }
        if (target !in allowed) throw ForbiddenException(Message.Orders.STATUS_NOT_ALLOWED)
    }

    fun assertSeller(userType: UserType) {
        if (userType != UserType.SELLER) throw ForbiddenException(Message.Orders.STATUS_NOT_ALLOWED)
    }
}
