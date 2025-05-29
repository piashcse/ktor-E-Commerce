package com.piashcse.feature.wishlist

import com.piashcse.database.entities.*
import com.piashcse.model.response.Product
import com.piashcse.utils.extension.alreadyExistException
import com.piashcse.utils.extension.notFoundException
import com.piashcse.utils.extension.query
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.and

/**
 * Controller for managing the user's wishlist. Provides methods to add, retrieve, and remove items from the wishlist.
 */
class WishListService : WishListRepository {

    /**
     * Adds a product to the user's wishlist. If the product is already in the wishlist, an exception is thrown.
     *
     * @param userId The ID of the user who wants to add the product to their wishlist.
     * @param productId The ID of the product to be added to the wishlist.
     * @return The product that was added to the wishlist.
     * @throws alreadyExistException If the product is already in the user's wishlist.
     */
    override suspend fun addToWishList(userId: String, productId: String): WishList = query {
        val isExits =
            WishListDAO.find { WishListTable.userId eq userId and (WishListTable.productId eq productId) }
                .toList()
                .singleOrNull()
        if (isExits == null) {
            WishListDAO.new {
                this.userId = EntityID(userId, WishListTable)
                this.productId = EntityID(productId, WishListTable)
            }.response(ProductDAO.find { ProductTable.id eq productId }.first().response())
        } else {
            throw productId.alreadyExistException()
        }
    }

    /**
     * Retrieves the list of products in the user's wishlist with a specified limit on the number of items.
     *
     * @param userId The ID of the user whose wishlist is to be retrieved.
     * @param limit The maximum number of products to retrieve.
     * @return A list of products in the user's wishlist.
     */
    override suspend fun getWishList(userId: String, limit: Int): List<Product> = query {
        WishListDAO.find { WishListTable.userId eq userId }.limit(limit).map {
            ProductDAO.find { ProductTable.id eq it.productId }.first().response()
        }
    }

    /**
     * Removes a product from the user's wishlist. If the product is not found in the wishlist, an exception is thrown.
     *
     * @param userId The ID of the user who wants to remove the product from their wishlist.
     * @param productId The ID of the product to be removed from the wishlist.
     * @return The product that was removed from the wishlist.
     * @throws notFoundException If the product is not found in the user's wishlist.
     */
    override suspend fun removeFromWishList(userId: String, productId: String): Product = query {
        val isExits =
            WishListDAO.find { WishListTable.userId eq userId and (WishListTable.productId eq productId) }
                .toList()
                .singleOrNull()
        isExits?.let {
            it.delete()
            ProductDAO.find { ProductTable.id eq it.productId }.first().response()
        } ?: run {
            throw productId.notFoundException()
        }
    }
}