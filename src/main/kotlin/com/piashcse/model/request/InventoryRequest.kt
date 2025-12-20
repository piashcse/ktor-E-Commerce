package com.piashcse.model.request

import kotlinx.serialization.Serializable

@Serializable
data class InventoryRequest(
    val productId: String,
    val shopId: String,
    val stockQuantity: Int,
    val reservedQuantity: Int = 0,
    val minimumStockLevel: Int? = null,
    val maximumStockLevel: Int? = null
)