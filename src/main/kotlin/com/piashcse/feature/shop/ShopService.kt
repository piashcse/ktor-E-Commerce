package com.piashcse.feature.shop

import com.piashcse.constants.Message
import com.piashcse.constants.ShopStatus
import com.piashcse.model.request.ShopRequest
import com.piashcse.model.request.UpdateShopRequest
import com.piashcse.model.response.ShopResponse
import com.piashcse.utils.common.PaginatedResponse
import com.piashcse.utils.extension.parseEnum
import com.piashcse.utils.extension.suspendRetryQuery
import com.piashcse.utils.validator.ForbiddenException

class ShopService(private val shopRepo: ShopRepository) {
    /**
     * Creates a new shop. Runs in a retryable transaction.
     */
    suspend fun createShop(
        userId: String,
        shopRequest: ShopRequest,
    ): ShopResponse = suspendRetryQuery {
        shopRepo.createShop(userId, shopRequest)
    }

    /**
     * Updates a shop after verifying ownership. Runs in a retryable transaction.
     */
    suspend fun updateShop(
        userId: String,
        shopId: String,
        shopRequest: UpdateShopRequest,
    ): ShopResponse = suspendRetryQuery {
        val access = shopRepo.getShopAccess(userId, shopId)
        if (!access.isOwner) throw ForbiddenException(Message.Errors.notOwner("shop"))
        shopRepo.updateShop(shopId, shopRequest)
    }

    suspend fun getShopById(shopId: String): ShopResponse? = shopRepo.getShopById(shopId)

    suspend fun getShopsByUser(
        userId: String,
        limit: Int = 20,
        offset: Int = 0,
    ): PaginatedResponse<ShopResponse> = shopRepo.getShopsByUser(userId, limit, offset)

    /**
     * Retrieves public shops, validating the status filter before querying.
     */
    suspend fun getShops(
        status: String? = null,
        category: String? = null,
        limit: Int = 20,
        offset: Int = 0,
    ): PaginatedResponse<ShopResponse> {
        val statusEnum = status?.parseEnum<ShopStatus>("status")
        return shopRepo.getShops(statusEnum, category, limit, offset)
    }

    suspend fun getShopsByCategory(
        categoryId: String,
        limit: Int = 20,
        offset: Int = 0,
    ): PaginatedResponse<ShopResponse> = shopRepo.getShopsByCategory(categoryId, limit, offset)

    suspend fun getFeaturedShops(
        limit: Int = 20,
        offset: Int = 0,
    ): PaginatedResponse<ShopResponse> = shopRepo.getFeaturedShops(limit, offset)

    suspend fun getShopsByStatus(
        status: ShopStatus,
        limit: Int = 20,
        offset: Int = 0,
    ): PaginatedResponse<ShopResponse> = shopRepo.getShopsByStatus(status, limit, offset)

    /**
     * Approves a shop application. Runs in a retryable transaction.
     */
    suspend fun approveShop(shopId: String): ShopResponse = suspendRetryQuery { shopRepo.approveShop(shopId) }

    /**
     * Rejects a shop application. Runs in a retryable transaction.
     */
    suspend fun rejectShop(shopId: String): ShopResponse = suspendRetryQuery { shopRepo.rejectShop(shopId) }

    /**
     * Suspends a shop. Runs in a retryable transaction.
     */
    suspend fun suspendShop(shopId: String): ShopResponse = suspendRetryQuery { shopRepo.suspendShop(shopId) }

    /**
     * Activates a suspended shop. Runs in a retryable transaction.
     */
    suspend fun activateShop(shopId: String): ShopResponse = suspendRetryQuery { shopRepo.activateShop(shopId) }
}
