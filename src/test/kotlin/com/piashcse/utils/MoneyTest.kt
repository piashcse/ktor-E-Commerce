package com.piashcse.utils

import com.piashcse.utils.common.Money
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MoneyTest {
    @Test
    fun `scale2 rounds half up`() {
        assertEquals(BigDecimal("19.99"), Money.scale2(BigDecimal("19.991")))
    }

    @Test
    fun `double conversion avoids binary residue`() {
        // 0.1 + 0.2 problem: BigDecimal.valueOf(0.1) path differs from String path in edge cases;
        // Money.of must be exact 2dp.
        assertEquals(BigDecimal("0.10"), Money.of(0.1))
    }

    @Test
    fun `discount capped at order amount`() {
        assertEquals(BigDecimal("5.00"), Money.cappedDiscount(BigDecimal("10.00"), BigDecimal("5.00")))
    }

    @Test
    fun `discount percent null when invalid`() {
        assertNull(Money.discountPercent(BigDecimal.ZERO, BigDecimal("1.00")))
    }

    @Test
    fun `unit total multiplies and scales`() {
        assertEquals(BigDecimal("59.97"), Money.unitTotal(BigDecimal("19.99"), 3))
    }
}
