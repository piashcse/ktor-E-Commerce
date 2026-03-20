package com.piashcse.model.response

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import com.piashcse.constants.InventoryStatus
import java.time.LocalDateTime

@Serializable
data class InventoryResponse(
    val id: String,
    val productId: String,
    val shopId: String,
    val stockQuantity: Int,
    val reservedQuantity: Int,
    val minimumStockLevel: Int,
    val maximumStockLevel: Int,
    val status: InventoryStatus,
    val lastRestocked: @Contextual LocalDateTime?,
    val createdAt: @Contextual LocalDateTime?,
    val updatedAt: @Contextual LocalDateTime?
)