package com.piashcse.mapper

import com.piashcse.database.entities.InventoryDAO
import com.piashcse.model.response.InventoryResponse

fun InventoryDAO.toInventoryResponse() = InventoryResponse(
    id = id.value,
    productId = productId.value,
    shopId = shopId.value,
    stockQuantity = stockQuantity,
    reservedQuantity = reservedQuantity,
    minimumStockLevel = minimumStockLevel,
    maximumStockLevel = maximumStockLevel,
    status = status,
    lastRestocked = lastRestocked,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
