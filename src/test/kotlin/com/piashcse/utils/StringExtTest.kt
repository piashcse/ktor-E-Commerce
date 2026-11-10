package com.piashcse.utils

import com.piashcse.constants.OrderStatus
import com.piashcse.utils.extension.parseEnum
import com.piashcse.utils.extension.requireValidName
import com.piashcse.utils.validator.InvalidEnumValueException
import com.piashcse.utils.validator.ValidationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class StringExtTest {
    @Test
    fun `parses enums case insensitively`() {
        assertEquals(OrderStatus.PAID, "paid".parseEnum<OrderStatus>("status"))
        assertEquals(OrderStatus.SHIPPED, "SHIPPED".parseEnum<OrderStatus>("status"))
    }

    @Test
    fun `invalid enum exposes allowed values`() {
        val error = assertFailsWith<InvalidEnumValueException> { "bogus".parseEnum<OrderStatus>("status") }
        assertEquals("INVALID_ENUM_VALUE", error.errorCode)
        assertEquals("bogus", error.invalidValue)
        assertEquals("PENDING, CONFIRMED, PAID, SHIPPED, DELIVERED, CANCELED, RECEIVED", error.enumName)
    }

    @Test
    fun `catalog names reject blank and over-long values`() {
        assertEquals(Unit, "Nike".requireValidName("Brand"))
        assertFailsWith<ValidationException> { " ".requireValidName("Brand") }
        assertFailsWith<ValidationException> { "x".repeat(256).requireValidName("Brand") }
    }
}
