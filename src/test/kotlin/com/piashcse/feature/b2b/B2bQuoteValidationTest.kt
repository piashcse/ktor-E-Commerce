package com.piashcse.feature.b2b

import com.piashcse.utils.validator.FieldValidationException
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertFailsWith

class B2bQuoteValidationTest {
    @Test
    fun `priced quote items reject non-positive values`() {
        assertFailsWith<FieldValidationException> {
            QuotePricedItem(productId = "p-1", quantity = 0, unitPrice = BigDecimal("10.00"))
        }
        assertFailsWith<FieldValidationException> {
            QuotePricedItem(productId = "p-1", quantity = 1, unitPrice = BigDecimal.ZERO)
        }
    }
}
