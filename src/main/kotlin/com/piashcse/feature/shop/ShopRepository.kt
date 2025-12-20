package com.piashcse.feature.shop

import com.piashcse.constants.ShopStatus
import com.piashcse.model.request.ShopRequest
import com.piashcse.model.request.UpdateShopRequest
import com.piashcse.model.response.Shop

interface ShopRepository {
    /**
     * Creates a new shop for a seller.
     *
     * @param userId The user ID creating the shop.
     * @param shopRequest The shop details to create.
     * @return The created shop.
     */
    suspend fun createShop(userId: String, shopRequest: ShopRequest): Shop

    /**
     * Updates an existing shop.
     *
     * @param userId The user ID updating the shop.
     * @param shopId The shop ID to update.
     * @param shopRequest The shop details to update.
     * @return The updated shop.
     */
    suspend fun updateShop(userId: String, shopId: String, shopRequest: UpdateShopRequest): Shop

    /**
     * Gets a shop by ID.
     *
     * @param shopId The shop ID to retrieve.
     * @return The shop details.
     */
    suspend fun getShopById(shopId: String): Shop?

    /**
     * Gets shops by user ID (seller).
     *
     * @param userId The user ID to retrieve shops for.
     * @return A list of shops for the user.
     */
    suspend fun getShopsByUser(userId: String): List<Shop>

    /**
     * Gets all shops with optional filtering.
     *
     * @param status Optional status to filter by.
     * @param category Optional category to filter by.
     * @param limit Number of shops to return.
     * @return A list of shops matching the criteria.
     */
    suspend fun getShops(status: String? = null, category: String? = null, limit: Int = 20): List<Shop>

    /**
     * Gets shops by category.
     *
     * @param categoryId The category ID to filter by.
     * @return A list of shops in the category.
     */
    suspend fun getShopsByCategory(categoryId: String): List<Shop>

    /**
     * Gets featured shops.
     *
     * @return A list of featured shops.
     */
    suspend fun getFeaturedShops(): List<Shop>

    /**
     * Gets shops by status.
     *
     * @param status The status to filter by.
     * @return A list of shops with the specified status.
     */
    suspend fun getShopsByStatus(status: ShopStatus): List<Shop>

    /**
     * Approves a shop application.
     *
     * @param shopId The shop ID to approve.
     * @return The updated shop.
     */
    suspend fun approveShop(shopId: String): Shop

    /**
     * Rejects a shop application.
     *
     * @param shopId The shop ID to reject.
     * @return The updated shop.
     */
    suspend fun rejectShop(shopId: String): Shop

    /**
     * Suspends a shop.
     *
     * @param shopId The shop ID to suspend.
     * @return The updated shop.
     */
    suspend fun suspendShop(shopId: String): Shop

    /**
     * Activates a suspended shop.
     *
     * @param shopId The shop ID to activate.
     * @return The updated shop.
     */
    suspend fun activateShop(shopId: String): Shop
}