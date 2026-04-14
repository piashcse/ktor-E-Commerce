package com.piashcse.model.request

import kotlinx.serialization.Serializable
import org.valiktor.functions.isGreaterThan
import org.valiktor.functions.isNotNull
import org.valiktor.functions.isNotEmpty
import org.valiktor.validate

@Serializable
data class UpdateCartItemRequest(
    val productId: String,
    val quantity: Int
) {
    fun validation() {
        validate(this) {
            validate(UpdateCartItemRequest::productId).isNotNull().isNotEmpty()
            validate(UpdateCartItemRequest::quantity).isGreaterThan(0)
        }
    }
}

@Serializable
data class RemoveCartItemRequest(
    val productId: String
) {
    fun validation() {
        validate(this) {
            validate(RemoveCartItemRequest::productId).isNotNull().isNotEmpty()
        }
    }
}
