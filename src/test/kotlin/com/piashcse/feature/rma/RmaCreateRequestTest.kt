package com.piashcse.feature.rma

import com.piashcse.constants.RmaType
import com.piashcse.utils.validator.ValidationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RmaCreateRequestTest {
    @Test
    fun `valid RMA request`() {
        val request = RmaCreateRequest("o-1", "oi-1", 1, RmaType.RETURN, "damaged")
        assertEquals(1, request.quantity)
    }

    @Test
    fun `RMA quantity must be positive`() {
        assertFailsWith<ValidationException> {
            RmaCreateRequest("o-1", "oi-1", 0, RmaType.RETURN, "damaged")
        }
    }

    @Test
    fun `RMA reason is required`() {
        assertFailsWith<ValidationException> {
            RmaCreateRequest("o-1", "oi-1", 1, RmaType.RETURN, " ")
        }
    }
}
