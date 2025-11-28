package com.piashcse.feature.inventory

import com.piashcse.model.request.InventoryRequest
import com.piashcse.model.response.InventoryResponse

interface InventoryRepository {
    /**
     * Creates or updates inventory for a product.
     *
     * @param inventoryRequest The inventory details to create or update.
     * @return The created/updated inventory record.
     */
    suspend fun createOrUpdateInventory(inventoryRequest: InventoryRequest): InventoryResponse

    /**
     * Gets inventory by product ID.
     *
     * @param productId The product ID to get inventory for.
     * @return The inventory record for the product.
     */
    suspend fun getInventoryByProduct(productId: String): InventoryResponse?

    /**
     * Updates inventory stock for a product.
     *
     * @param productId The product ID to update.
     * @param quantity The quantity to update by.
     * @param operation The operation to perform: "add", "subtract", or "set".
     * @return The updated inventory record.
     */
    suspend fun updateStock(productId: String, quantity: Int, operation: String = "add"): InventoryResponse

    /**
     * Gets low stock products.
     *
     * @param limit The maximum number of products to return.
     * @return A list of products with low stock.
     */
    suspend fun getLowStockProducts(limit: Int = 10): List<InventoryResponse>

    /**
     * Gets inventory by shop ID.
     *
     * @param shopId The shop ID to get inventory for.
     * @return A list of inventory records for the shop.
     */
    suspend fun getInventoryByShop(shopId: String): List<InventoryResponse>
}