package com.piashcse.model.request

import kotlinx.serialization.Serializable
import org.valiktor.functions.isGreaterThan
import org.valiktor.functions.isNotNull
import org.valiktor.functions.isNotEmpty
import org.valiktor.validate

@Serializable
data class UpdateStockRequest(
    val quantity: Int,
    val operation: String = "add" // add, subtract, set
) {
    fun validation() {
        validate(this) {
            validate(UpdateStockRequest::quantity).isGreaterThan(-1)
            validate(UpdateStockRequest::operation).isNotNull().isNotEmpty()
        }
    }
}
