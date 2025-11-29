package com.piashcse.feature.cart

import com.piashcse.database.entities.*
import com.piashcse.model.response.Product
import com.piashcse.utils.ValidationException
import com.piashcse.utils.extension.alreadyExistException
import com.piashcse.utils.extension.notFoundException
import com.piashcse.utils.extension.query
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.eq

/**
 * Controller for managing cart-related operations.
 */
class CartService : CartRepository {

    /**
     * Creates a new cart item for a user with the specified product and quantity.
     *
     * @param userId The ID of the user for whom the cart item is being created.
     * @param productId The ID of the product to be added to the cart.
     * @param quantity The quantity of the product being added to the cart.
     * @return The created cart item entity.
     * @throws Exception if the product already exists in the user's cart.
     */
    override suspend fun createCart(userId: String, productId: String, quantity: Int): Cart = query {
        validateCartInput(userId, productId, quantity)

        val existingCartItem = CartItemDAO.find {
            CartItemTable.userId eq userId and (CartItemTable.productId eq productId)
        }.singleOrNull()
        existingCartItem?.let {
            throw productId.alreadyExistException()
        } ?: CartItemDAO.new {
            this.userId = EntityID(userId, CartItemTable)
            this.productId = EntityID(productId, CartItemTable)
            this.quantity = quantity
        }.response()
    }

    private fun validateCartInput(userId: String, productId: String, quantity: Int) {
        if (userId.isBlank()) throw ValidationException("User ID cannot be blank")
        if (productId.isBlank()) throw ValidationException("Product ID cannot be blank")
        if (quantity <= 0) throw ValidationException("Quantity must be greater than 0")
    }

    /**
     * Retrieves a list of cart items for a user, with a specified limit.
     *
     * @param userId The ID of the user for whom to retrieve cart items.
     * @param limit The maximum number of cart items to retrieve.
     * @return A list of cart item entities with associated product details.
     */
    override suspend fun getCartItems(userId: String, limit: Int): List<Cart> = query {
        CartItemDAO.find { CartItemTable.userId eq userId }.limit(limit).map {
            it.response(ProductDAO.find { ProductTable.id eq it.productId }.first().response())
        }
    }

    /**
     * Updates the quantity of an existing cart item.
     *
     * @param userId The ID of the user whose cart item quantity is to be updated.
     * @param productId The ID of the product for which the quantity is being updated.
     * @param quantity The amount to update the product quantity by (can be positive or negative).
     * @return The updated cart item entity with the new quantity.
     * @throws Exception if the product does not exist in the user's cart.
     */
    override suspend fun updateCartQuantity(userId: String, productId: String, quantity: Int): Cart = query {
        if (userId.isBlank()) throw ValidationException("User ID cannot be blank")
        if (productId.isBlank()) throw ValidationException("Product ID cannot be blank")

        val cartItem = CartItemDAO.find {
            CartItemTable.userId eq userId and (CartItemTable.productId eq productId)
        }.singleOrNull() ?: throw productId.notFoundException()

        // Calculate new quantity, ensuring it doesn't go below 0
        val newQuantity = (cartItem.quantity + quantity).coerceAtLeast(0)
        cartItem.quantity = newQuantity

        // If quantity becomes 0, remove the item from cart
        if (newQuantity == 0) {
            cartItem.delete()
            throw productId.notFoundException()
        }

        val product = ProductDAO.findById(cartItem.productId) ?:
            throw "Product not found".notFoundException()

        cartItem.response(product.response())
    }

    /**
     * Removes a product from a user's cart.
     *
     * @param userId The ID of the user from whose cart the product is to be removed.
     * @param productId The ID of the product to be removed from the cart.
     * @return The product entity that was removed from the cart.
     * @throws Exception if the product does not exist in the user's cart.
     */
    override suspend fun removeCartItem(userId: String, productId: String): Product = query {
        if (userId.isBlank()) throw ValidationException("User ID cannot be blank")
        if (productId.isBlank()) throw ValidationException("Product ID cannot be blank")

        val cartItem = CartItemDAO.find {
            CartItemTable.userId eq userId and (CartItemTable.productId eq productId)
        }.singleOrNull() ?: throw productId.notFoundException()

        val product = ProductDAO.findById(cartItem.productId) ?:
            throw "Product not found".notFoundException()

        cartItem.delete()
        product.response()
    }

    /**
     * Clears all items from a user's cart.
     *
     * @param userId The ID of the user whose cart is to be cleared.
     * @return True if the cart was cleared successfully.
     */
    override suspend fun clearCart(userId: String): Boolean = query {
        if (userId.isBlank()) throw ValidationException("User ID cannot be blank")

        val cartItems = CartItemDAO.find { CartItemTable.userId eq userId }.toList()
        cartItems.forEach { it.delete() }
        true
    }
}