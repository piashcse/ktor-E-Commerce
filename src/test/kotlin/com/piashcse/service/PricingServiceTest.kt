package com.piashcse.service

import com.piashcse.utils.common.Money
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals

class PricingServiceTest {
    @Test
    fun `breakdown adds shipping and tax`() {
        val b = PricingService.breakdown(BigDecimal("100.00"), BigDecimal("10.00"))
        assertEquals(BigDecimal("5.00"), b.taxAmount)
        assertEquals(BigDecimal("115.00"), b.total)
    }

    @Test
    fun `breakdown never negative with oversized discount`() {
        val b = PricingService.breakdown(BigDecimal("5.00"), BigDecimal.ZERO, BigDecimal("10.00"))
        assertEquals(BigDecimal("0.00"), b.total)
    }

    @Test
    fun `money discount percent`() {
        assertEquals(BigDecimal("10.00"), Money.discountPercent(BigDecimal("100.00"), BigDecimal("90.00")))
    }
}
