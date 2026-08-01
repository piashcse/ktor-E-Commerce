package com.piashcse.feature.inventory

import com.piashcse.model.request.InventoryRequest
import com.piashcse.model.response.InventoryResponse
import com.piashcse.utils.common.PaginatedResponse

class InventoryService(private val inventoryRepo: InventoryRepository) {
    suspend fun createOrUpdateInventory(inventoryRequest: InventoryRequest): InventoryResponse =
        inventoryRepo.createOrUpdateInventory(inventoryRequest)

    suspend fun getInventoryByProduct(productId: String): InventoryResponse? = inventoryRepo.getInventoryByProduct(productId)

    suspend fun updateStock(
        productId: String,
        quantity: Int,
        operation: String = "add",
    ): InventoryResponse = inventoryRepo.updateStock(productId, quantity, operation)

    suspend fun getLowStockProducts(
        limit: Int = 10,
        offset: Int = 0,
    ): PaginatedResponse<InventoryResponse> = inventoryRepo.getLowStockProducts(limit, offset)

    suspend fun getInventoryByShop(
        shopId: String,
        limit: Int = 20,
        offset: Int = 0,
    ): PaginatedResponse<InventoryResponse> = inventoryRepo.getInventoryByShop(shopId, limit, offset)
}
