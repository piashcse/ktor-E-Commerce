package com.piashcse.feature.wishlist

import com.piashcse.model.response.ProductResponse
import com.piashcse.model.response.WishListResponse
import com.piashcse.utils.common.PaginatedResponse
import com.piashcse.utils.extension.suspendRetryQuery

class WishListService(private val wishlistRepo: WishListRepository) {
    /**
     * Adds a product to the user's wish list. Runs in a retryable transaction.
     */
    suspend fun addToWishList(
        userId: String,
        productId: String,
    ): WishListResponse = suspendRetryQuery { wishlistRepo.addToWishList(userId, productId) }

    suspend fun getWishList(
        userId: String,
        limit: Int,
        offset: Int,
    ): PaginatedResponse<ProductResponse> = wishlistRepo.getWishList(userId, limit, offset)

    /**
     * Removes a product from the user's wish list. Runs in a retryable transaction.
     */
    suspend fun removeFromWishList(
        userId: String,
        productId: String,
    ): ProductResponse = suspendRetryQuery { wishlistRepo.removeFromWishList(userId, productId) }

    suspend fun isProductInWishList(
        userId: String,
        productId: String,
    ): Boolean = wishlistRepo.isProductInWishList(userId, productId)
}
