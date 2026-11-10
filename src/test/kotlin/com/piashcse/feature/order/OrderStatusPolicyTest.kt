package com.piashcse.feature.order

import com.piashcse.constants.OrderStatus
import com.piashcse.constants.UserType
import com.piashcse.utils.validator.ForbiddenException
import kotlin.test.Test
import kotlin.test.assertFailsWith

class OrderStatusPolicyTest {
    @Test
    fun `seller may confirm ship and deliver`() {
        listOf(OrderStatus.CONFIRMED, OrderStatus.SHIPPED, OrderStatus.DELIVERED).forEach {
            OrderStatusPolicy.assertCanSet(UserType.SELLER, it)
        }
    }

    @Test
    fun `customer may cancel or mark received`() {
        listOf(OrderStatus.CANCELED, OrderStatus.RECEIVED).forEach {
            OrderStatusPolicy.assertCanSet(UserType.CUSTOMER, it)
        }
    }

    @Test
    fun `customer cannot confirm seller transitions`() {
        assertFailsWith<ForbiddenException> {
            OrderStatusPolicy.assertCanSet(UserType.CUSTOMER, OrderStatus.SHIPPED)
        }
    }

    @Test
    fun `seller cannot cancel a customer order`() {
        assertFailsWith<ForbiddenException> {
            OrderStatusPolicy.assertCanSet(UserType.SELLER, OrderStatus.CANCELED)
        }
    }

    @Test
    fun `admin cannot use the customer seller status endpoint`() {
        assertFailsWith<ForbiddenException> {
            OrderStatusPolicy.assertCanSet(UserType.ADMIN, OrderStatus.CONFIRMED)
        }
    }

    @Test
    fun `only sellers may ship`() {
        OrderStatusPolicy.assertSeller(UserType.SELLER)
        assertFailsWith<ForbiddenException> { OrderStatusPolicy.assertSeller(UserType.CUSTOMER) }
    }
}
