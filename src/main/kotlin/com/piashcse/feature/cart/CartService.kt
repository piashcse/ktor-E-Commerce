package com.piashcse.feature.cart

import com.piashcse.constants.Message
import com.piashcse.model.response.CartResponse
import com.piashcse.model.response.CartSummaryResponse
import com.piashcse.model.response.ProductResponse
import com.piashcse.utils.common.PaginatedResponse
import com.piashcse.utils.extension.requireNotBlank
import com.piashcse.utils.extension.suspendRetryQuery
import com.piashcse.utils.validator.ValidationException

class CartService(private val cartRepo: CartRepository) {

    private fun requireCartParams(
        userId: String,
        productId: String,
        quantity: Int? = null,
    ) {
        userId.requireNotBlank("User ID")
        productId.requireNotBlank("Product ID")
        if (quantity != null && quantity <= 0) throw ValidationException(Message.Validation.notPositive("Quantity"))
    }

    /**
     * Adds an item to the cart after validating the quantity. Runs in a
     * retryable transaction.
     */
    suspend fun createCart(
        userId: String,
        productId: String,
        quantity: Int,
    ): CartResponse = suspendRetryQuery {
        requireCartParams(userId, productId, quantity)
        cartRepo.createCart(userId, productId, quantity)
    }

    suspend fun getCartItems(
        userId: String,
        limit: Int,
        offset: Int = 0,
    ): PaginatedResponse<CartResponse> = cartRepo.getCartItems(userId, limit, offset)

    /**
     * Updates the quantity of a cart item, removing it when the quantity is zero.
     * Runs in a retryable transaction.
     */
    suspend fun updateCartQuantity(
        userId: String,
        productId: String,
        quantity: Int,
    ): CartResponse? = suspendRetryQuery {
        requireCartParams(userId, productId)
        if (quantity == 0) {
            cartRepo.removeCartItem(userId, productId)
            null
        } else {
            cartRepo.updateCartQuantity(userId, productId, quantity)
        }
    }

    /**
     * Removes an item from the cart. Runs in a retryable transaction.
     */
    suspend fun removeCartItem(
        userId: String,
        productId: String,
    ): ProductResponse = suspendRetryQuery {
        requireCartParams(userId, productId)
        cartRepo.removeCartItem(userId, productId)
    }

    /**
     * Clears the user's cart. Runs in a retryable transaction.
     */
    suspend fun clearCart(userId: String): Boolean = suspendRetryQuery {
        userId.requireNotBlank("User ID")
        cartRepo.clearCart(userId)
    }

    suspend fun getCartSummary(userId: String): CartSummaryResponse = cartRepo.getCartSummary(userId)
}
