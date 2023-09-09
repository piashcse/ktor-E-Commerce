package com.piashcse.models.cart

import com.papsign.ktor.openapigen.annotations.parameters.QueryParam
import org.valiktor.functions.isGreaterThan
import org.valiktor.functions.isNotEmpty
import org.valiktor.functions.isNotNull
import org.valiktor.validate

data class AddCart(
    @QueryParam("productId") val productId: String,
    @QueryParam("quantity") val quantity: Int
) {
    fun validation() {
        validate(this) {
            validate(AddCart::productId).isNotNull().isNotEmpty()
            validate(AddCart::quantity).isNotNull().isGreaterThan(0)
        }
    }
}
