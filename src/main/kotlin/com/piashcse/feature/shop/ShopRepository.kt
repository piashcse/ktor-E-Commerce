package com.piashcse.feature.shop

import com.piashcse.constants.ShopStatus
import com.piashcse.model.request.ShopRequest
import com.piashcse.model.request.UpdateShopRequest
import com.piashcse.model.response.ShopResponse
import com.piashcse.utils.common.PaginatedResponse

/**
 * Facts about a shop used by the service layer to enforce authorization.
 */
data class ShopAccess(
    val shopId: String,
    val isOwner: Boolean,
)

/**
 * Persistence boundary for the Shop aggregate.
 *
 * The repository is limited to data access (reads/writes + projection to DTOs).
 * All authorization, validation and transaction orchestration live in
 * [ShopService].
 */
interface ShopRepository {
    /**
     * Creates a new shop for a seller. Assumes the caller has already authorized
     * the seller; enforces the one-shop-per-user rule within the transaction.
     *
     * @param userId The user ID creating the shop.
     * @param shopRequest The shop details to create.
     * @return The created shop.
     */
    suspend fun createShop(
        userId: String,
        shopRequest: ShopRequest,
    ): ShopResponse

    /**
     * Resolves the authorization facts for a shop relative to [userId].
     * Throws if the shop does not exist.
     */
    suspend fun getShopAccess(
        userId: String,
        shopId: String,
    ): ShopAccess

    /**
     * Updates an existing shop. Assumes the caller has authorized the shop.
     *
     * @param shopId The shop ID to update.
     * @param shopRequest The shop details to update.
     * @return The updated shop.
     */
    suspend fun updateShop(
        shopId: String,
        shopRequest: UpdateShopRequest,
    ): ShopResponse

    /**
     * Gets a shop by ID.
     *
     * @param shopId The shop ID to retrieve.
     * @return The shop details.
     */
    suspend fun getShopById(shopId: String): ShopResponse?

    /**
     * Gets shops by user ID (seller).
     *
     * @param userId The user ID to retrieve shops for.
     * @param limit Number of shops to return.
     * @param offset Number of shops to skip.
     * @return A list of shops for the user.
     */
    suspend fun getShopsByUser(
        userId: String,
        limit: Int = 20,
        offset: Int = 0,
    ): PaginatedResponse<ShopResponse>

    /**
     * Gets all shops with optional filtering. The caller is responsible for
     * parsing and validating the status string.
     *
     * @param status Optional status to filter by.
     * @param category Optional category to filter by.
     * @param limit Number of shops to return.
     * @param offset Number of shops to skip.
     * @return A list of shops matching the criteria.
     */
    suspend fun getShops(
        status: ShopStatus? = null,
        category: String? = null,
        limit: Int = 20,
        offset: Int = 0,
    ): PaginatedResponse<ShopResponse>

    /**
     * Gets shops by category.
     *
     * @param categoryId The category ID to filter by.
     * @param limit Number of shops to return.
     * @param offset Number of shops to skip.
     * @return A list of shops in the category.
     */
    suspend fun getShopsByCategory(
        categoryId: String,
        limit: Int = 20,
        offset: Int = 0,
    ): PaginatedResponse<ShopResponse>

    /**
     * Gets featured shops.
     *
     * @param limit Number of shops to return.
     * @param offset Number of shops to skip.
     * @return A list of featured shops.
     */
    suspend fun getFeaturedShops(
        limit: Int = 20,
        offset: Int = 0,
    ): PaginatedResponse<ShopResponse>

    /**
     * Gets shops by status.
     *
     * @param status The status to filter by.
     * @param limit Number of shops to return.
     * @param offset Number of shops to skip.
     * @return A list of shops with the specified status.
     */
    suspend fun getShopsByStatus(
        status: ShopStatus,
        limit: Int = 20,
        offset: Int = 0,
    ): PaginatedResponse<ShopResponse>

    /**
     * Approves a shop application.
     *
     * @param shopId The shop ID to approve.
     * @return The updated shop.
     */
    suspend fun approveShop(shopId: String): ShopResponse

    /**
     * Rejects a shop application.
     *
     * @param shopId The shop ID to reject.
     * @return The updated shop.
     */
    suspend fun rejectShop(shopId: String): ShopResponse

    /**
     * Suspends a shop.
     *
     * @param shopId The shop ID to suspend.
     * @return The updated shop.
     */
    suspend fun suspendShop(shopId: String): ShopResponse

    /**
     * Activates a suspended shop.
     *
     * @param shopId The shop ID to activate.
     * @return The updated shop.
     */
    suspend fun activateShop(shopId: String): ShopResponse
}
