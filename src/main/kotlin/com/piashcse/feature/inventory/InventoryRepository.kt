package com.piashcse.feature.inventory

import com.piashcse.model.request.InventoryRequest
import com.piashcse.model.response.InventoryResponse
import com.piashcse.utils.common.PaginatedResponse

interface InventoryRepository {
    /**
     * Creates or updates inventory for a product owned by the seller's shop.
     *
     * @param actorUserId Authenticated seller; must own the product/shop.
     */
    suspend fun createOrUpdateInventory(
        inventoryRequest: InventoryRequest,
        actorUserId: String,
    ): InventoryResponse

    /**
     * Gets inventory by product ID, scoped to the seller's shop.
     */
    suspend fun getInventoryByProduct(
        productId: String,
        actorUserId: String,
        variantId: String? = null,
    ): InventoryResponse?

    /**
     * Updates inventory stock for a product owned by the seller's shop.
     */
    suspend fun updateStock(
        actorUserId: String,
        productId: String,
        quantity: Int,
        operation: String = "add",
        warehouseId: String? = null,
        variantId: String? = null,
    ): InventoryResponse

    /**
     * Gets low-stock products for the seller's shop.
     */
    suspend fun getLowStockProducts(
        actorUserId: String,
        limit: Int = 10,
        offset: Int = 0,
    ): PaginatedResponse<InventoryResponse>

    /**
     * Gets inventory for a shop; the caller must own that shop.
     */
    suspend fun getInventoryByShop(
        actorUserId: String,
        shopId: String,
        limit: Int = 20,
        offset: Int = 0,
    ): PaginatedResponse<InventoryResponse>

    /**
     * Gets all inventory records across shops (admin only).
     */
    suspend fun getAllInventory(
        limit: Int = 20,
        offset: Int = 0,
        shopId: String? = null,
    ): PaginatedResponse<InventoryResponse>
}
