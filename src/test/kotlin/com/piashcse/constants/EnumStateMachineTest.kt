package com.piashcse.constants

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EnumStateMachineTest {
    @Test
    fun `order transitions follow the happy path`() {
        assertTrue(OrderStatus.canTransitionTo(OrderStatus.PENDING, OrderStatus.CONFIRMED))
        assertTrue(OrderStatus.canTransitionTo(OrderStatus.CONFIRMED, OrderStatus.PAID))
        assertTrue(OrderStatus.canTransitionTo(OrderStatus.PAID, OrderStatus.SHIPPED))
        assertTrue(OrderStatus.canTransitionTo(OrderStatus.SHIPPED, OrderStatus.DELIVERED))
        assertTrue(OrderStatus.canTransitionTo(OrderStatus.DELIVERED, OrderStatus.RECEIVED))
    }

    @Test
    fun `paid orders cannot be canceled without a refund`() {
        assertFalse(OrderStatus.canTransitionTo(OrderStatus.PAID, OrderStatus.CANCELED))
        assertTrue(OrderStatus.canBeCanceled(OrderStatus.PENDING))
        assertTrue(OrderStatus.canBeCanceled(OrderStatus.CONFIRMED))
        assertFalse(OrderStatus.canBeCanceled(OrderStatus.PAID))
        assertFalse(OrderStatus.canBeCanceled(OrderStatus.DELIVERED))
    }

    @Test
    fun `terminal order states cannot transition`() {
        assertFalse(OrderStatus.canTransitionTo(OrderStatus.CANCELED, OrderStatus.PENDING))
        assertFalse(OrderStatus.canTransitionTo(OrderStatus.RECEIVED, OrderStatus.DELIVERED))
    }

    @Test
    fun `rma transitions follow the return lifecycle`() {
        assertTrue(RmaStatus.canTransitionTo(RmaStatus.REQUESTED, RmaStatus.APPROVED))
        assertTrue(RmaStatus.canTransitionTo(RmaStatus.APPROVED, RmaStatus.IN_TRANSIT))
        assertTrue(RmaStatus.canTransitionTo(RmaStatus.IN_TRANSIT, RmaStatus.RECEIVED))
        assertTrue(RmaStatus.canTransitionTo(RmaStatus.RECEIVED, RmaStatus.COMPLETED))
        assertFalse(RmaStatus.canTransitionTo(RmaStatus.COMPLETED, RmaStatus.REQUESTED))
        assertFalse(RmaStatus.canTransitionTo(RmaStatus.REJECTED, RmaStatus.APPROVED))
    }

    @Test
    fun `payment status flags`() {
        assertFalse(PaymentStatus.PENDING.isFinal)
        assertTrue(PaymentStatus.COMPLETED.isFinal)
        assertTrue(PaymentStatus.FAILED.isFinal)
        assertTrue(PaymentStatus.REFUNDED.isFinal)
        assertTrue(PaymentStatus.COMPLETED.isSuccessful)
        assertFalse(PaymentStatus.PENDING.isSuccessful)
    }

    @Test
    fun `inventory status derives from stock levels`() {
        assertEquals(InventoryStatus.OUT_OF_STOCK, InventoryStatus.fromStockLevel(0, 10))
        assertEquals(InventoryStatus.LOW_STOCK, InventoryStatus.fromStockLevel(10, 10))
        assertEquals(InventoryStatus.IN_STOCK, InventoryStatus.fromStockLevel(11, 10))
        assertTrue(InventoryStatus.IN_STOCK.isAvailable)
        assertTrue(InventoryStatus.LOW_STOCK.needsAttention)
        assertTrue(InventoryStatus.OUT_OF_STOCK.needsAttention)
    }

    @Test
    fun `shop and product status helpers`() {
        assertTrue(ShopStatus.APPROVED.isOperational)
        assertTrue(ShopStatus.APPROVED.isActive)
        assertFalse(ShopStatus.PENDING.isOperational)
        assertTrue(ProductStatus.ACTIVE.isActive)
        assertFalse(ProductStatus.OUT_OF_STOCK.isActive)
    }
}
