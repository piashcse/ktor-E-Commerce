package com.piashcse.model.request

import kotlinx.serialization.Serializable
import org.valiktor.functions.isGreaterThan
import org.valiktor.functions.isNotNull
import org.valiktor.functions.isNotEmpty
import org.valiktor.validate

@Serializable
data class InventoryRequest(
    val productId: String,
    val shopId: String,
    val stockQuantity: Int,
    val reservedQuantity: Int = 0,
    val minimumStockLevel: Int? = null,
    val maximumStockLevel: Int? = null
) {
    fun validation() {
        validate(this) {
            validate(InventoryRequest::productId).isNotNull().isNotEmpty()
            validate(InventoryRequest::shopId).isNotNull().isNotEmpty()
            validate(InventoryRequest::stockQuantity).isGreaterThan(-1)
            validate(InventoryRequest::reservedQuantity).isGreaterThan(-1)
        }
    }
}
