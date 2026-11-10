package com.piashcse.feature.product

import com.piashcse.constants.ProductStatus
import com.piashcse.utils.validator.ValidationException
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ProductVariantRequestTest {
    @Test
    fun `valid variant request`() {
        val request =
            ProductVariantRequest(
                sku = "TSHIRT-RED-M",
                name = "Red / M",
                price = BigDecimal("19.99"),
                discountPrice = BigDecimal("15.99"),
                status = ProductStatus.ACTIVE,
            )
        assertEquals("TSHIRT-RED-M", request.sku)
    }

    @Test
    fun `price must be positive`() {
        assertFailsWith<ValidationException> {
            ProductVariantRequest("SKU", "Variant", BigDecimal.ZERO)
        }
    }

    @Test
    fun `discount must be lower than price`() {
        assertFailsWith<ValidationException> {
            ProductVariantRequest("SKU", "Variant", BigDecimal("10.00"), BigDecimal("10.00"))
        }
    }

    @Test
    fun `option name and value are required`() {
        assertFailsWith<ValidationException> { ProductOptionRequest(" ") }
        assertFailsWith<ValidationException> { ProductOptionValueRequest(" ") }
    }
}
