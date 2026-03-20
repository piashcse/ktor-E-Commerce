package com.piashcse.model.request

import kotlinx.serialization.Serializable
import org.valiktor.functions.isGreaterThan
import org.valiktor.functions.isNotEmpty
import org.valiktor.functions.isNotNull
import org.valiktor.validate

@Serializable
data class CartRequest(
    val productId: String,
    val quantity: Int
) {
    fun validation() {
        validate(this) {
            validate(CartRequest::productId).isNotNull().isNotEmpty()
            validate(CartRequest::quantity).isNotNull().isGreaterThan(0)
        }
    }
}