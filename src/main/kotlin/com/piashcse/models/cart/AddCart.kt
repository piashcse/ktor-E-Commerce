package com.piashcse.models.cart

import org.valiktor.functions.isGreaterThan
import org.valiktor.functions.isNotEmpty
import org.valiktor.functions.isNotNull
import org.valiktor.validate

data class AddCart(
    val productId: String,
    val quantity: Int
) {
    fun validation() {
        validate(this) {
            validate(AddCart::productId).isNotNull().isNotEmpty()
            validate(AddCart::quantity).isNotNull().isGreaterThan(0)
        }
    }
}
