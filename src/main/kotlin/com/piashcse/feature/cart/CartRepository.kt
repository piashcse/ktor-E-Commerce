package com.piashcse.feature.cart

import com.piashcse.model.response.CartResponse
import com.piashcse.model.response.CartSummaryResponse
import com.piashcse.model.response.ProductResponse
import com.piashcse.utils.common.PaginatedResponse

/**
 * Persistence boundary for the Cart aggregate.
 *
 * The repository is limited to data access (reads/writes + projection to DTOs).
 * All validation and transaction orchestration live in [CartService].
 */
interface CartRepository {
    /**
     * Adds a product to the cart or rejects a duplicate. Assumes the caller has
     * validated the quantity.
     *
     * @param userId The unique identifier of the user.
     * @param productId The unique identifier of the product.
     * @param quantity The quantity of the product to add.
     * @return The updated cart.
     */
    suspend fun createCart(
        userId: String,
        productId: String,
        quantity: Int,
    ): CartResponse

    /**
     * Retrieves all cart items for a user.
     *
     * @param userId The unique identifier of the user.
     * @param limit The maximum number of cart items to return.
     * @return A list of cart items.
     */
    suspend fun getCartItems(
        userId: String,
        limit: Int,
        offset: Int = 0,
    ): PaginatedResponse<CartResponse>

    /**
     * Sets the quantity of a specific product in the cart. Assumes the caller
     * has handled the remove-on-zero rule.
     *
     * @param userId The unique identifier of the user.
     * @param productId The unique identifier of the product.
     * @param quantity The new quantity of the product.
     * @return The updated cart.
     */
    suspend fun updateCartQuantity(
        userId: String,
        productId: String,
        quantity: Int,
    ): CartResponse?

    /**
     * Removes a specific product from the cart.
     *
     * @param userId The unique identifier of the user.
     * @param productId The unique identifier of the product.
     * @return The removed product.
     */
    suspend fun removeCartItem(
        userId: String,
        productId: String,
    ): ProductResponse

    /**
     * Clears all items from a user's cart.
     *
     * @param userId The unique identifier of the user.
     * @return `true` if the cart was cleared successfully, `false` otherwise.
     */
    suspend fun clearCart(userId: String): Boolean

    /**
     * Returns a summary of the user's cart including items, subtotal, tax, and item count.
     *
     * @param userId The unique identifier of the user.
     * @return CartSummaryResponse containing cart details.
     */
    suspend fun getCartSummary(userId: String): CartSummaryResponse
}
