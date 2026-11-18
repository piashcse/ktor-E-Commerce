package com.piashcse.mapper

import com.piashcse.database.entities.InventoryDAO
import com.piashcse.model.response.InventoryResponse

fun InventoryDAO.toInventoryResponse(
    productName: String? = null,
    sku: String? = null,
) = InventoryResponse(
    id = id.value,
    productId = productId.value,
    productName = productName,
    sku = sku,
    variantId = variantId?.value,
    shopId = shopId.value,
    warehouseId = warehouseId.value,
    stockQuantity = stockQuantity,
    reservedQuantity = reservedQuantity,
    minimumStockLevel = minimumStockLevel,
    maximumStockLevel = maximumStockLevel,
    status = status,
    lastRestocked = lastRestocked,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
