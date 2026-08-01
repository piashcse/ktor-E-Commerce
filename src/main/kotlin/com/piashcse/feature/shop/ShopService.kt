package com.piashcse.feature.shop

import com.piashcse.constants.ShopStatus
import com.piashcse.model.request.ShopRequest
import com.piashcse.model.request.UpdateShopRequest
import com.piashcse.model.response.ShopResponse
import com.piashcse.utils.common.PaginatedResponse

class ShopService(private val shopRepo: ShopRepository) {
    suspend fun createShop(
        userId: String,
        shopRequest: ShopRequest,
    ): ShopResponse = shopRepo.createShop(userId, shopRequest)

    suspend fun updateShop(
        userId: String,
        shopId: String,
        shopRequest: UpdateShopRequest,
    ): ShopResponse = shopRepo.updateShop(userId, shopId, shopRequest)

    suspend fun getShopById(shopId: String): ShopResponse? = shopRepo.getShopById(shopId)

    suspend fun getShopsByUser(
        userId: String,
        limit: Int = 20,
        offset: Int = 0,
    ): PaginatedResponse<ShopResponse> = shopRepo.getShopsByUser(userId, limit, offset)

    suspend fun getShops(
        status: String? = null,
        category: String? = null,
        limit: Int = 20,
        offset: Int = 0,
    ): PaginatedResponse<ShopResponse> = shopRepo.getShops(status, category, limit, offset)

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

    suspend fun approveShop(shopId: String): ShopResponse = shopRepo.approveShop(shopId)

    suspend fun rejectShop(shopId: String): ShopResponse = shopRepo.rejectShop(shopId)

    suspend fun suspendShop(shopId: String): ShopResponse = shopRepo.suspendShop(shopId)

    suspend fun activateShop(shopId: String): ShopResponse = shopRepo.activateShop(shopId)
}
