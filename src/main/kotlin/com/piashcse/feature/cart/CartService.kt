package com.piashcse.feature.cart

import com.piashcse.model.response.CartResponse
import com.piashcse.model.response.CartSummaryResponse
import com.piashcse.model.response.ProductResponse
import com.piashcse.utils.common.PaginatedResponse

class CartService(private val cartRepo: CartRepository) {
    suspend fun createCart(
        userId: String,
        productId: String,
        quantity: Int,
    ): CartResponse = cartRepo.createCart(userId, productId, quantity)

    suspend fun getCartItems(
        userId: String,
        limit: Int,
        offset: Int = 0,
    ): PaginatedResponse<CartResponse> = cartRepo.getCartItems(userId, limit, offset)

    suspend fun updateCartQuantity(
        userId: String,
        productId: String,
        quantity: Int,
    ): CartResponse? = cartRepo.updateCartQuantity(userId, productId, quantity)

    suspend fun removeCartItem(
        userId: String,
        productId: String,
    ): ProductResponse = cartRepo.removeCartItem(userId, productId)

    suspend fun clearCart(userId: String): Boolean = cartRepo.clearCart(userId)

    suspend fun getCartSummary(userId: String): CartSummaryResponse = cartRepo.getCartSummary(userId)
}
