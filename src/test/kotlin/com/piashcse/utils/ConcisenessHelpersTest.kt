package com.piashcse.utils

import com.piashcse.constants.UserType
import com.piashcse.utils.common.Money
import com.piashcse.utils.extension.requireOrderAccess
import com.piashcse.utils.extension.requireValidName
import com.piashcse.utils.validator.ForbiddenException
import com.piashcse.utils.validator.ValidationException
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ConcisenessHelpersTest {
    @Test
    fun `percentOf computes 25 percent`() {
        assertEquals(BigDecimal("50.00"), Money.percentOf(BigDecimal("200.00"), BigDecimal("25")))
    }

    @Test
    fun `commission delegates to percentOf`() {
        assertEquals(BigDecimal("10.00"), Money.commission(BigDecimal("100.00"), BigDecimal("10.00")))
    }

    @Test
    fun `proportionalSplit divides fairly`() {
        assertEquals(BigDecimal("3.33"), Money.proportionalSplit(BigDecimal("10.00"), BigDecimal("1"), BigDecimal("3")))
    }

    @Test
    fun `proportionalSplit zero total is zero`() {
        assertEquals(BigDecimal("0.00"), Money.proportionalSplit(BigDecimal("10.00"), BigDecimal("1"), BigDecimal.ZERO))
    }

    @Test
    fun `str renders 2dp`() {
        assertEquals("19.90", Money.str(BigDecimal("19.9")))
    }

    @Test
    fun `requireValidName rejects blank and oversize`() {
        assertFailsWith<ValidationException> { "".requireValidName("Brand") }
        assertFailsWith<ValidationException> { "x".repeat(256).requireValidName("Brand") }
        "Apple".requireValidName("Brand")
    }

    @Test
    fun `requireOrderAccess allows owner seller admin`() {
        requireOrderAccess("u1", "s1", "u1", UserType.CUSTOMER)
        requireOrderAccess("u9", "s1", "u2", UserType.ADMIN)
    }

    @Test
    fun `requireOrderAccess rejects stranger`() {
        assertFailsWith<ForbiddenException> {
            requireOrderAccess("owner", null, "stranger", UserType.CUSTOMER)
        }
    }
}
