package com.piashcse.feature.tax

import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TaxServiceTest {
    private val service = TaxService()

    @Test
    fun `exclusive tax adds to the net amount`() {
        val amounts = service.apply(BigDecimal("100.00"), TaxQuote(BigDecimal("0.15"), inclusive = false))
        assertEquals(0, amounts.net.compareTo(BigDecimal("100.00")))
        assertEquals(0, amounts.tax.compareTo(BigDecimal("15.00")))
        assertEquals(0, amounts.gross.compareTo(BigDecimal("115.00")))
    }

    @Test
    fun `inclusive tax is carved out of the gross amount`() {
        val amounts = service.apply(BigDecimal("115.00"), TaxQuote(BigDecimal("0.15"), inclusive = true))
        assertEquals(0, amounts.net.compareTo(BigDecimal("100.00")))
        assertEquals(0, amounts.tax.compareTo(BigDecimal("15.00")))
        assertEquals(0, amounts.gross.compareTo(BigDecimal("115.00")))
    }

    @Test
    fun `inclusive tax rounds to cents`() {
        val amounts = service.apply(BigDecimal("10.00"), TaxQuote(BigDecimal("0.05"), inclusive = true))
        assertEquals(0, amounts.net.compareTo(BigDecimal("9.52")))
        assertEquals(0, amounts.tax.compareTo(BigDecimal("0.48")))
        assertEquals(0, amounts.gross.compareTo(BigDecimal("10.00")))
    }

    @Test
    fun `exempt and zero-rated lines produce no tax`() {
        val exempt = service.apply(BigDecimal("50.00"), TaxQuote(BigDecimal("0.15"), inclusive = true, exempt = true))
        assertEquals(0, exempt.tax.compareTo(BigDecimal.ZERO))
        assertEquals(0, exempt.net.compareTo(BigDecimal("50.00")))
        val zero = service.apply(BigDecimal("50.00"), TaxQuote(BigDecimal.ZERO, inclusive = false))
        assertEquals(0, zero.tax.compareTo(BigDecimal.ZERO))
        assertEquals(0, zero.gross.compareTo(BigDecimal("50.00")))
    }

    @Test
    fun `negative and zero amounts stay untouched`() {
        val zero = service.apply(BigDecimal.ZERO, TaxQuote(BigDecimal("0.10"), inclusive = false))
        assertTrue(zero.tax.compareTo(BigDecimal.ZERO) == 0)
        assertEquals(0, zero.net.compareTo(BigDecimal.ZERO))
    }
}
