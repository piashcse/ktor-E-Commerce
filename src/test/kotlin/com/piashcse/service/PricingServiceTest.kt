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

    @Test
    fun `currency conversion rounds to cents`() {
        assertEquals(BigDecimal("92.35"), PricingService.toCurrency(BigDecimal("100.00"), BigDecimal("0.9235")))
    }

    @Test
    fun `line total converts the unit before multiplying`() {
        assertEquals(
            BigDecimal("27.72"),
            PricingService.lineTotal(BigDecimal("9.235"), 3, BigDecimal("1.0000")),
        )
        assertEquals(
            BigDecimal("46.18"),
            PricingService.lineTotal(BigDecimal("10.00"), 2, BigDecimal("2.309")),
        )
    }
}
