package com.piashcse.feature.inventory

import com.piashcse.model.request.InventoryRequest
import com.piashcse.model.response.InventoryResponse
import com.piashcse.utils.common.PaginatedResponse

/**
 * Locked read of an inventory row used by [InventoryService] to compute the
 * new stock level under a row lock.
 */
data class InventoryStockInfo(
    val productId: String,
    val stockQuantity: Int,
    val minimumStockLevel: Int,
    val maximumStockLevel: Int,
)

/**
 * Access facts used by [InventoryService] to authorize seller inventory
 * operations against a specific shop.
 */
data class InventoryAccess(
    val isSellerOwner: Boolean,
    val isAdmin: Boolean,
)

/**
 * Persistence boundary for the Inventory feature.
 *
 * The repository is limited to data access (reads/writes + projection).
 * Validation and authorization live in [InventoryService]; stock arithmetic
 * for `updateStock` and status computation from resolved stock levels live in
 * [InventoryService], while [createOrUpdateInventory] computes the status after
 * resolving the persisted minimum/maximum stock levels for the product+shop key.
 */
interface InventoryRepository {
    /**
     * Locks the inventory row for a product and returns its current stock.
     */
    suspend fun getInventoryForUpdate(productId: String): InventoryStockInfo

    /**
     * Resolves whether the user is the seller-owner of a shop or an admin.
     */
    suspend fun getInventoryAccess(userId: String, shopId: String): InventoryAccess

    /**
     * Returns the shop id of the inventory row for a product, or null.
     */
    suspend fun getInventoryShopId(productId: String): String?

    /**
     * Returns the shop id the product belongs to, or null if it has none.
     */
    suspend fun getProductShopId(productId: String): String?

    /**
     * Returns the shop id linked to a seller, or null if the seller has no shop.
     */
    suspend fun getSellerShopId(userId: String): String?

    /**
     * Persists a new stock quantity and status for a product.
     */
    suspend fun setStock(
        productId: String,
        newStock: Int,
        status: com.piashcse.constants.InventoryStatus,
    ): InventoryResponse

    /**
     * Creates or updates inventory for a product.
     */
    suspend fun createOrUpdateInventory(inventoryRequest: InventoryRequest): InventoryResponse

    /**
     * Gets inventory by product ID.
     */
    suspend fun getInventoryByProduct(productId: String): InventoryResponse?

    /**
     * Gets low stock products, optionally scoped to a single shop (null = all shops).
     */
    suspend fun getLowStockProducts(
        shopId: String?,
        limit: Int = 10,
        offset: Int = 0,
    ): PaginatedResponse<InventoryResponse>

    /**
     * Gets inventory by shop ID.
     */
    suspend fun getInventoryByShop(
        shopId: String,
        limit: Int = 20,
        offset: Int = 0,
    ): PaginatedResponse<InventoryResponse>
}
