package com.piashcse.feature.tax

import org.valiktor.ConstraintViolationException
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TaxRuleRequestTest {
    @Test
    fun `accepts rates between zero and one`() {
        assertEquals(BigDecimal.ZERO, TaxRuleRequest(country = "BD", rate = BigDecimal.ZERO).rate)
        assertEquals(BigDecimal.ONE, TaxRuleRequest(country = "BD", rate = BigDecimal.ONE).rate)
    }

    @Test
    fun `rejects negative and above-one rates`() {
        assertFailsWith<ConstraintViolationException> { TaxRuleRequest(country = "BD", rate = BigDecimal("-0.01")) }
        assertFailsWith<ConstraintViolationException> { TaxRuleRequest(country = "BD", rate = BigDecimal("1.01")) }
    }

    @Test
    fun `rejects blank country`() {
        assertFailsWith<ConstraintViolationException> { TaxRuleRequest(country = " ", rate = BigDecimal("0.05")) }
    }
}
