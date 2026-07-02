package com.piashcse.model.request

import kotlinx.serialization.Serializable
import org.valiktor.functions.isGreaterThanOrEqualTo
import org.valiktor.functions.isNotEmpty
import org.valiktor.functions.isNotNull
import org.valiktor.validate

@Serializable
data class CartRequest(
    val productId: String,
    val quantity: Int,
) {
    init {
        validate(this) {
            validate(CartRequest::productId).isNotNull().isNotEmpty()
            validate(CartRequest::quantity).isNotNull().isGreaterThanOrEqualTo(0)
        }
    }
}
