package com.example.models.cart

import com.example.models.category.AddCategory
import org.valiktor.functions.isGreaterThan
import org.valiktor.functions.isNotEmpty
import org.valiktor.functions.isNotNull
import org.valiktor.validate

data class AddCart(
    val productId: String,
    val totalPrice: Float,
    val singlePrice: Float,
    val quantity: Int
) {
    fun validation() {
        validate(this) {
            validate(AddCart::productId).isNotNull().isNotEmpty()
            validate(AddCart::totalPrice).isNotNull().isGreaterThan(0f)
            validate(AddCart::singlePrice).isNotNull().isGreaterThan(0f)
            validate(AddCart::quantity).isNotNull().isGreaterThan(0)
        }
    }
}
