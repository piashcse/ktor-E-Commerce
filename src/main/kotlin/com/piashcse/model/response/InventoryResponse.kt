package com.piashcse.model.response

import com.piashcse.constants.InventoryStatus
import java.time.LocalDateTime

data class InventoryResponse(
    val id: String,
    val productId: String,
    val shopId: String,
    val stockQuantity: Int,
    val reservedQuantity: Int,
    val minimumStockLevel: Int,
    val maximumStockLevel: Int,
    val status: InventoryStatus,
    val lastRestocked: LocalDateTime?,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?
)