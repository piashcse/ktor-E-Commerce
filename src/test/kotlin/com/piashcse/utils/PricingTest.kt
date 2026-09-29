package com.piashcse.utils

import com.piashcse.utils.common.Money
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.test.Test
import kotlin.test.assertEquals

class PricingTest {
    @Test
    fun `percentage discount capped by max`() {
        val order = BigDecimal("200.00")
        val pct = BigDecimal("25")
        val raw = order.multiply(pct).divide(BigDecimal(100), 10, RoundingMode.HALF_UP)
        val capped = raw.min(BigDecimal("30.00"))
        assertEquals(BigDecimal("30.00"), capped.setScale(2, RoundingMode.HALF_UP))
    }

    @Test
    fun `fixed coupon capped at order total`() {
        assertEquals(BigDecimal("5.00"), Money.cappedDiscount(BigDecimal("10.00"), BigDecimal("5.00")))
    }

    @Test
    fun `tax 5 percent`() {
        assertEquals(BigDecimal("5.00"), Money.tax(BigDecimal("100.00"), BigDecimal("0.05")))
    }
}
