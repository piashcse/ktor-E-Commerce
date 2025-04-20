package com.piashcse.modules.cart.controller

import com.piashcse.database.entities.Cart
import com.piashcse.database.entities.CartItemDAO
import com.piashcse.database.entities.CartItemTable
import com.piashcse.database.entities.Product
import com.piashcse.database.entities.ProductDAO
import com.piashcse.database.entities.ProductTable
import com.piashcse.modules.cart.repository.CartRepo
import com.piashcse.utils.extension.alreadyExistException
import com.piashcse.utils.extension.notFoundException
import com.piashcse.utils.extension.query
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.and

/**
 * Controller for managing cart-related operations.
 */
class CartController : CartRepo {

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
        val isProductExist =
            CartItemDAO.Companion.find { CartItemTable.userId eq userId and (CartItemTable.productId eq productId) }
                .toList().singleOrNull()
        isProductExist?.let {
            throw productId.alreadyExistException()
        } ?: CartItemDAO.Companion.new {
            this.userId = EntityID(userId, CartItemTable)
            this.productId = EntityID(productId, CartItemTable)
            this.quantity = quantity
        }.response()
    }

    /**
     * Retrieves a list of cart items for a user, with a specified limit.
     *
     * @param userId The ID of the user for whom to retrieve cart items.
     * @param limit The maximum number of cart items to retrieve.
     * @return A list of cart item entities with associated product details.
     */
    override suspend fun getCartItems(userId: String, limit: Int): List<Cart> = query {
        CartItemDAO.Companion.find { CartItemTable.userId eq userId }.limit(limit).map {
            it.response(ProductDAO.Companion.find { ProductTable.id eq it.productId }.first().response())
        }
    }

    /**
     * Updates the quantity of an existing cart item.
     *
     * @param userId The ID of the user whose cart item quantity is to be updated.
     * @param productId The ID of the product for which the quantity is being updated.
     * @param quantity The amount to update the product quantity by.
     * @return The updated cart item entity with the new quantity.
     * @throws Exception if the product does not exist in the user's cart.
     */
    override suspend fun updateCartQuantity(userId: String, productId: String, quantity: Int): Cart = query {
        val isProductExist =
            CartItemDAO.Companion.find { CartItemTable.userId eq userId and (CartItemTable.productId eq productId) }
                .toList().singleOrNull()
        isProductExist?.let {
            it.quantity = it.quantity + quantity
            it.response(ProductDAO.Companion.find { ProductTable.id eq it.productId }.first().response())
        } ?: throw productId.notFoundException()
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
        val isProductExist =
            CartItemDAO.Companion.find { CartItemTable.userId eq userId and (CartItemTable.productId eq productId) }
                .toList().singleOrNull()
        isProductExist?.let {
            it.delete()
            ProductDAO.Companion.find { ProductTable.id eq it.productId }.first().response()
        } ?: throw productId.notFoundException()
    }

    /**
     * Clears all items from a user's cart.
     *
     * @param userId The ID of the user whose cart is to be cleared.
     * @return True if the cart was cleared successfully, false if the cart was already empty.
     */
    override suspend fun clearCart(userId: String): Boolean = query {
        val isCartExist = CartItemDAO.Companion.find { CartItemTable.userId eq userId }.toList()
        if (isCartExist.isEmpty()) {
            true
        } else {
            isCartExist.forEach {
                it.delete()
            }
            true
        }
    }
}