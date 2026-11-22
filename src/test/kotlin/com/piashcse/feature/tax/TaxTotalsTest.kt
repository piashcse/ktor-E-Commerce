package com.piashcse.feature.tax

import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TaxTotalsTest {
    private val exclusive = TaxQuote(BigDecimal("0.10"), inclusive = false)
    private val inclusive = TaxQuote(BigDecimal("0.10"), inclusive = true)

    @Test
    fun `exclusive lines accumulate tax and taxable add-on`() {
        val totals = TaxTotals().add(BigDecimal("100.00"), exclusive).add(BigDecimal("50.00"), exclusive)
        assertEquals(0, totals.tax.compareTo(BigDecimal("15.00")))
        assertEquals(0, totals.exclusiveTax.compareTo(BigDecimal("15.00")))
        assertFalse(totals.inclusive)
    }

    @Test
    fun `inclusive lines do not add to the exclusive add-on`() {
        val totals = TaxTotals().add(BigDecimal("110.00"), inclusive)
        assertEquals(0, totals.tax.compareTo(BigDecimal("10.00")))
        assertEquals(0, totals.exclusiveTax.compareTo(BigDecimal.ZERO))
        assertTrue(totals.inclusive)
    }

    @Test
    fun `mixed lines track both tax and inclusive flag`() {
        val totals = TaxTotals().add(BigDecimal("110.00"), inclusive).add(BigDecimal("100.00"), exclusive)
        assertEquals(0, totals.tax.compareTo(BigDecimal("20.00")))
        assertEquals(0, totals.exclusiveTax.compareTo(BigDecimal("10.00")))
        assertTrue(totals.inclusive)
    }

    @Test
    fun `normalized totals use two decimals`() {
        val totals = TaxTotals(tax = BigDecimal("1.234"), exclusiveTax = BigDecimal("1.2")).normalized()
        assertEquals(BigDecimal("1.23"), totals.tax)
        assertEquals(BigDecimal("1.20"), totals.exclusiveTax)
    }
}
