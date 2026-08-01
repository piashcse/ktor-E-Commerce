package com.piashcse.feature.wishlist

import com.piashcse.model.response.ProductResponse
import com.piashcse.model.response.WishListResponse
import com.piashcse.utils.common.PaginatedResponse

class WishListService(private val wishlistRepo: WishListRepository) {
    suspend fun addToWishList(
        userId: String,
        productId: String,
    ): WishListResponse = wishlistRepo.addToWishList(userId, productId)

    suspend fun getWishList(
        userId: String,
        limit: Int,
        offset: Int,
    ): PaginatedResponse<ProductResponse> = wishlistRepo.getWishList(userId, limit, offset)

    suspend fun removeFromWishList(
        userId: String,
        productId: String,
    ): ProductResponse = wishlistRepo.removeFromWishList(userId, productId)

    suspend fun isProductInWishList(
        userId: String,
        productId: String,
    ): Boolean = wishlistRepo.isProductInWishList(userId, productId)
}
