package com.piashcse.feature.wishlist

import com.piashcse.database.entities.*
import com.piashcse.mapper.toProductResponse
import com.piashcse.mapper.toWishListResponse
import com.piashcse.model.response.ProductResponse
import com.piashcse.utils.common.PaginatedResponse
import com.piashcse.utils.extension.*
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.selectAll

class WishListRepositoryImpl : WishListRepository {
    override suspend fun addToWishList(
        userId: String,
        productId: String,
    ): WishList =
        query {
            val product = ProductDAO.findById(productId) ?: productId.throwNotFound("ProductResponse")

            val existing =
                WishListDAO.find { WishListTable.userId eq userId and (WishListTable.productId eq productId) }
                    .firstOrNull()

            if (existing == null) {
                WishListDAO.new {
                    this.userId = userId.entityID(WishListTable)
                    this.productId = productId.entityID(ProductTable)
                }.toWishListResponse(product.toProductResponse())
            } else {
                throw productId.throwConflict("ProductResponse")
            }
        }

    override suspend fun getWishList(
        userId: String,
        limit: Int,
        offset: Int,
    ): PaginatedResponse<ProductResponse> =
        query {
            WishListTable.innerJoin(ProductTable)
                .selectAll()
                .andWhere { WishListTable.userId eq userId }
                .toPaginatedResponse(limit, offset) {
                    ProductDAO.wrapRow(it).toProductResponse()
                }
        }

    override suspend fun removeFromWishList(
        userId: String,
        productId: String,
    ): ProductResponse =
        query {
            val wishListItem =
                WishListDAO.find { WishListTable.userId eq userId and (WishListTable.productId eq productId) }
                    .firstOrNull() ?: productId.throwNotFound("ProductResponse")
            val product = ProductDAO.findById(productId)?.toProductResponse() ?: productId.throwNotFound("ProductResponse")
            wishListItem.delete()
            product
        }

    override suspend fun isProductInWishList(
        userId: String,
        productId: String,
    ): Boolean =
        query {
            !WishListDAO.find { WishListTable.userId eq userId and (WishListTable.productId eq productId) }
                .empty()
        }
}
