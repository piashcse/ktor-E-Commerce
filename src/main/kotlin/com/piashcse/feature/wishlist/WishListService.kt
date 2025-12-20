package com.piashcse.feature.wishlist

import com.piashcse.database.entities.*
import com.piashcse.model.response.Product
import com.piashcse.utils.extension.alreadyExistException
import com.piashcse.utils.extension.notFoundException
import com.piashcse.utils.extension.query
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.selectAll

/**
 * Controller for managing the user's wishlist. Provides methods to add, retrieve, and remove items from the wishlist.
 */
class WishListService : WishListRepository {

    /**
     * Adds a product to the user's wishlist. If the product is already in the wishlist, an exception is thrown.
     * Checks if the product exists before adding.
     *
     * @param userId The ID of the user who wants to add the product to their wishlist.
     * @param productId The ID of the product to be added to the wishlist.
     * @return The product that was added to the wishlist.
     * @throws alreadyExistException If the product is already in the user's wishlist.
     * @throws notFoundException If the product does not exist.
     */
    override suspend fun addToWishList(userId: String, productId: String): WishList = query {
        val product = ProductDAO.findById(productId) ?: throw productId.notFoundException()
        
        val isExits = WishListDAO.find { WishListTable.userId eq userId and (WishListTable.productId eq productId) }
                .toList()
                .singleOrNull()
        
        if (isExits == null) {
            WishListDAO.new {
                this.userId = EntityID(userId, WishListTable)
                this.productId = EntityID(productId, WishListTable)
            }.response(product.response())
        } else {
            throw productId.alreadyExistException()
        }
    }

    /**
     * Retrieves the list of products in the user's wishlist using pagination.
     * Uses a Join to fetch products efficiently.
     *
     * @param userId The ID of the user whose wishlist is to be retrieved.
     * @param limit The maximum number of products to retrieve.
     * @param offset The number of items to skip.
     * @return A list of products in the user's wishlist.
     */
    override suspend fun getWishList(userId: String, limit: Int, offset: Long): List<Product> = query {
        WishListTable.innerJoin(ProductTable)
            .selectAll()
            .andWhere { WishListTable.userId eq userId }
            .limit(limit)
            .offset(offset)
            .map { ProductDAO.wrapRow(it).response() }
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
        val wishListItem = WishListDAO.find { WishListTable.userId eq userId and (WishListTable.productId eq productId) }
            .singleOrNull() ?: throw productId.notFoundException()
        
        val productResponse = ProductDAO.findById(productId)?.response()?: throw productId.notFoundException()
        wishListItem.delete()
        productResponse
    }

    override suspend fun isProductInWishList(userId: String, productId: String): Boolean = query {
         WishListDAO.find { WishListTable.userId eq userId and (WishListTable.productId eq productId) }
            .limit(1)
            .count() > 0
    }
}