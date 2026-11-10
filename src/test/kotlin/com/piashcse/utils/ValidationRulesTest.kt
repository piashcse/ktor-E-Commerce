package com.piashcse.utils

import com.piashcse.utils.validator.ValidationException
import com.piashcse.utils.validator.requirePositivePrice
import com.piashcse.utils.validator.requireStrongPassword
import com.piashcse.utils.validator.validateDiscountPrice
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ValidationRulesTest {
    @Test
    fun `strong password helper accepts valid passwords`() {
        "Password123!".requireStrongPassword()
    }

    @Test
    fun `strong password helper rejects weak passwords`() {
        assertFailsWith<ValidationException> { "weak".requireStrongPassword() }
    }

    @Test
    fun `positive price helper returns the value or throws`() {
        assertEquals(BigDecimal("10.00"), BigDecimal("10.00").requirePositivePrice())
        assertFailsWith<ValidationException> { BigDecimal.ZERO.requirePositivePrice("Price") }
        assertFailsWith<ValidationException> { BigDecimal("-1.00").requirePositivePrice("Price") }
    }

    @Test
    fun `discount price must be positive and below the regular price`() {
        validateDiscountPrice(BigDecimal("20.00"), BigDecimal("15.00"))
        validateDiscountPrice(BigDecimal("20.00"), null)
        assertFailsWith<ValidationException> { validateDiscountPrice(BigDecimal("20.00"), BigDecimal.ZERO) }
        assertFailsWith<ValidationException> { validateDiscountPrice(BigDecimal("20.00"), BigDecimal("20.00")) }
        assertFailsWith<ValidationException> { validateDiscountPrice(BigDecimal("20.00"), BigDecimal("25.00")) }
    }
}
